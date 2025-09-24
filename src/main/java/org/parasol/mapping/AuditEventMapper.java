package org.parasol.mapping;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;

import org.parasol.model.audit.AuditSource;
import org.parasol.model.audit.LLMInitialMessagesCreatedAuditEvent;
import org.parasol.model.audit.LLMInteractionCompleteAuditEvent;
import org.parasol.model.audit.LLMInteractionFailedAuditEvent;
import org.parasol.model.audit.LLMResponseReceivedAuditEvent;
import org.parasol.model.audit.ToolExecutedAuditEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import io.quarkiverse.langchain4j.audit.AuditSourceInfo;
import io.quarkiverse.langchain4j.audit.InitialMessagesCreatedEvent;
import io.quarkiverse.langchain4j.audit.LLMInteractionCompleteEvent;
import io.quarkiverse.langchain4j.audit.LLMInteractionFailureEvent;
import io.quarkiverse.langchain4j.audit.ResponseFromLLMReceivedEvent;
import io.quarkiverse.langchain4j.audit.ToolExecutedEvent;

@ApplicationScoped
public class AuditEventMapper {
	private final ObjectMapper objectMapper;

	public AuditEventMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public LLMInitialMessagesCreatedAuditEvent toAuditEvent(InitialMessagesCreatedEvent initialMessagesCreatedEvent) {
		return LLMInitialMessagesCreatedAuditEvent.builder()
			.sourceInfo(toAuditSource(initialMessagesCreatedEvent.sourceInfo()))
			.systemMessage(fromSystemMessage(initialMessagesCreatedEvent.systemMessage()))
			.userMessage(fromUserMessage(initialMessagesCreatedEvent.userMessage()))
			.build();
	}

	public LLMInteractionCompleteAuditEvent toAuditEvent(LLMInteractionCompleteEvent llmInteractionCompleteEvent) {
		return LLMInteractionCompleteAuditEvent.builder()
			.sourceInfo(toAuditSource(llmInteractionCompleteEvent.sourceInfo()))
			.result(toJson(llmInteractionCompleteEvent.result()))
			.build();
	}

	public LLMInteractionFailedAuditEvent toAuditEvent(LLMInteractionFailureEvent llmInteractionFailureEvent) {
		return LLMInteractionFailedAuditEvent.builder()
			.sourceInfo(toAuditSource(llmInteractionFailureEvent.sourceInfo()))
			.errorMessage(getMessage(llmInteractionFailureEvent.error()))
			.causeErrorMessage(getCauseMessage(llmInteractionFailureEvent.error()))
			.build();
	}

	public LLMResponseReceivedAuditEvent toAuditEvent(ResponseFromLLMReceivedEvent responseFromLLMReceivedEvent) {
		return LLMResponseReceivedAuditEvent.builder()
			.sourceInfo(toAuditSource(responseFromLLMReceivedEvent.sourceInfo()))
			.response(fromResponse(responseFromLLMReceivedEvent.response()))
			.modelName(responseFromLLMReceivedEvent.response().modelName())
			.inputTokenCount(responseFromLLMReceivedEvent.response().tokenUsage().inputTokenCount())
			.outputTokenCount(responseFromLLMReceivedEvent.response().tokenUsage().outputTokenCount())
			.build();
	}

	public ToolExecutedAuditEvent toAuditEvent(ToolExecutedEvent toolExecutedEvent) {
		return ToolExecutedAuditEvent.builder()
			.sourceInfo(toAuditSource(toolExecutedEvent.sourceInfo()))
			.result(toolExecutedEvent.result())
			.toolName(toolExecutedEvent.request().name())
			.toolArgs(toolExecutedEvent.request().arguments())
			.build();
	}

//	public InputGuardrailExecutedAuditEvent toAuditEvent(InputGuardrailExecutedEvent inputGuardrailExecutedEvent) {
//		return InputGuardrailExecutedAuditEvent.builder()
//			.sourceInfo(toAuditSource(inputGuardrailExecutedEvent.sourceInfo()))
//			.userMessage(fromUserMessage(inputGuardrailExecutedEvent.params().userMessage()))
//			.rewrittenUserMessage(fromUserMessage(inputGuardrailExecutedEvent.rewrittenUserMessage()))
//			.result(inputGuardrailExecutedEvent.result().result().name())
//			.guardrailClass(inputGuardrailExecutedEvent.guardrailClass().getName())
//			.build();
//	}
//
//	public OutputGuardrailExecutedAuditEvent toAuditEvent(OutputGuardrailExecutedEvent outputGuardrailExecutedEvent) {
//		return OutputGuardrailExecutedAuditEvent.builder()
//			.sourceInfo(toAuditSource(outputGuardrailExecutedEvent.sourceInfo()))
//			.response(Optional.ofNullable(outputGuardrailExecutedEvent.params().responseFromLLM()).map(AiMessage::text).orElse(null))
//			.result(outputGuardrailExecutedEvent.result().result().name())
//			.guardrailClass(outputGuardrailExecutedEvent.guardrailClass().getName())
//			.build();
//	}

	private AuditSource toAuditSource(AuditSourceInfo auditSourceInfo) {
		return AuditSource.builder()
			.interfaceName(auditSourceInfo.interfaceName())
			.methodName(auditSourceInfo.methodName())
			.interactionId(auditSourceInfo.interactionId())
			.build();
	}

	private static String fromResponse(ChatResponse response) {
		return Optional.ofNullable(response)
			.map(r -> r.aiMessage().text())
			.or(() ->
				Optional.ofNullable(response.aiMessage().toolExecutionRequests())
					.map(List::stream)
					.flatMap(Stream::findFirst)
					.map(toolExecutionRequest -> "EXECUTE TOOL: %s(%s)".formatted(toolExecutionRequest.name(), toolExecutionRequest.arguments()))
			)
			.orElse(null);
	}

	private static String fromSystemMessage(Optional<SystemMessage> systemMessage) {
		return systemMessage.map(SystemMessage::text).orElse("");
	}

	private static String fromUserMessage(UserMessage userMessage) {
		return (userMessage != null) ?
		       userMessage.singleText() :
		       null;
	}

	String toJson(Object object) {
		try {
			return this.objectMapper.writeValueAsString(object);
		}
		catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

		//		return this.objectMapper.valueToTree(object);
//		return this.objectMapper.convertValue(object, new TypeReference<>() {});
//		return switch (object) {
//			case null -> null;
//			case String s -> Map.of("string", s);
//			case Number n -> Map.of("number", String.valueOf(n));
//			case Boolean b2 -> Map.of("boolean", String.valueOf(b2));
//			case Object o when o.getClass().isPrimitive() -> Map.of("value", String.valueOf(o));
//			case Object o when o.getClass().isArray() -> Map.of("array", String.valueOf(o));
//			default -> this.objectMapper.convertValue(object, new TypeReference<>() {});
//		};
	}

	private static String getMessage(Throwable t) {
		return Optional.ofNullable(t)
			.map(Throwable::getMessage)
			.orElse(null);
	}

	private static String getCauseMessage(Throwable t) {
		return Optional.ofNullable(t)
			.map(Throwable::getCause)
			.map(AuditEventMapper::getMessage)
			.orElse(null);
	}
}
