package org.parasol.repository;

import java.util.Arrays;
import java.util.Optional;

import jakarta.annotation.Priority;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import dev.langchain4j.model.chat.response.ChatResponseMetadata;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.quarkiverse.langchain4j.audit.AuditSourceInfo;
import io.quarkiverse.langchain4j.audit.LLMInteractionEvent;
import io.quarkiverse.langchain4j.audit.ResponseFromLLMReceivedEvent;
import io.quarkiverse.langchain4j.audit.ToolExecutedEvent;

@AuditObserved
@Interceptor
@Priority(Interceptor.Priority.APPLICATION + 200)
public class AuditingObservabilityInterceptor {
	private final Meter meter;
	private final Tracer tracer;

	public AuditingObservabilityInterceptor(Meter meter, Tracer tracer) {
		this.meter = meter;
		this.tracer = tracer;
	}

	@AroundInvoke
	public Object invoke(InvocationContext context) throws Exception {
		var auditObservedAnnotation = getAuditObservedAnnotation(context);

		if (auditObservedAnnotation.isPresent()) {
			var auditObserved = auditObservedAnnotation.get();
			var interactionEvent = getInteractionEvent(context);
			var sourceInfo = interactionEvent.map(LLMInteractionEvent::sourceInfo).orElseThrow();
			var spanAttributes = Attributes.builder()
			                               .put("arg.interfaceName", sourceInfo.interfaceName())
			                               .put("arg.methodName", sourceInfo.methodName());
			var metricAttributes = Attributes.builder()
			                                 .put("interfaceName", sourceInfo.interfaceName())
			                                 .put("methodName", sourceInfo.methodName());

			interactionEvent.filter(event -> event instanceof ToolExecutedEvent)
			                .map(ToolExecutedEvent.class::cast)
			                .map(event -> event.request().name())
			                .ifPresent(toolName -> {
				                spanAttributes.put("arg.toolName", toolName);
				                metricAttributes.put("toolName", toolName);
			                });

			var span = this.tracer.spanBuilder(auditObserved.name())
			                      .setParent(Context.current().with(Span.current()))
			                      .setSpanKind(SpanKind.INTERNAL)
			                      .setAllAttributes(spanAttributes.build())
			                      .startSpan();

			try {
				return context.proceed();
			}
			finally {
				span.end();

				addToCounter(
					auditObserved.name(),
					auditObserved.description(),
					auditObserved.unit(),
					sourceInfo
				);

				interactionEvent.filter(event -> event instanceof ResponseFromLLMReceivedEvent)
				                .map(ResponseFromLLMReceivedEvent.class::cast)
				                .map(event -> event.response().metadata())
				                .ifPresent(this::addToTotalTokenCount);
			}
		}

		return context.proceed();
	}

	private Optional<LLMInteractionEvent> getInteractionEvent(InvocationContext context) {
		return Arrays.stream(context.getParameters())
		             .filter(param -> param instanceof LLMInteractionEvent)
		             .map(LLMInteractionEvent.class::cast)
		             .findFirst();
	}

	private Optional<AuditObserved> getAuditObservedAnnotation(InvocationContext context) {
		return context.getInterceptorBindings().stream()
		              .filter(annotation -> annotation instanceof AuditObserved)
		              .map(AuditObserved.class::cast)
		              .filter(auditObserved -> !auditObserved.name().strip().isBlank())
		              .findFirst();
	}

	private void addToCounter(String name, String description, String unit, AuditSourceInfo sourceInfo) {
		this.meter.counterBuilder(name)
		          .setDescription(description)
		          .setUnit(unit)
		          .build()
		          .add(
			          1,
			          Attributes.builder()
			                    .put("interfaceName", sourceInfo.interfaceName())
			                    .put("methodName", sourceInfo.methodName())
			                    .build()
		          );
	}

	private void addToTotalTokenCount(ChatResponseMetadata metadata) {
		var modelNameAttributes = Attributes.of(AttributeKey.stringKey("modelName"), metadata.modelName());

		this.meter.counterBuilder("parasol.llm.token.input.count")
		          .setDescription("Total input token count")
		          .setUnit("tokens")
		          .build()
		          .add(metadata.tokenUsage().inputTokenCount(), modelNameAttributes);

		this.meter.counterBuilder("parasol.llm.token.output.count")
		          .setDescription("Total output token count")
		          .setUnit("tokens")
		          .build()
		          .add(metadata.tokenUsage().outputTokenCount(), modelNameAttributes);

		this.meter.counterBuilder("parasol.llm.token.total.count")
		          .setDescription("Total token count")
		          .setUnit("tokens")
		          .build()
		          .add(metadata.tokenUsage().totalTokenCount(), modelNameAttributes);
	}
}
