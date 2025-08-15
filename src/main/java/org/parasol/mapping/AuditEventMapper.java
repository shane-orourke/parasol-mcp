package org.parasol.mapping;

import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants.ComponentModel;
import org.parasol.model.audit.AuditSource;
import org.parasol.model.audit.InputGuardrailExecutedAuditEvent;
import org.parasol.model.audit.LLMInteractionCompleteAuditEvent;
import org.parasol.model.audit.LLMInteractionFailedAuditEvent;
import org.parasol.model.audit.LLMResponseReceivedAuditEvent;
import org.parasol.model.audit.OutputGuardrailExecutedAuditEvent;
import org.parasol.model.audit.ToolExecutedAuditEvent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkiverse.langchain4j.audit.AuditSourceInfo;
import io.quarkiverse.langchain4j.audit.InputGuardrailExecutedEvent;
import io.quarkiverse.langchain4j.audit.LLMInteractionCompleteEvent;
import io.quarkiverse.langchain4j.audit.LLMInteractionFailureEvent;
import io.quarkiverse.langchain4j.audit.OutputGuardrailExecutedEvent;
import io.quarkiverse.langchain4j.audit.ResponseFromLLMReceivedEvent;
import io.quarkiverse.langchain4j.audit.ToolExecutedEvent;

@Mapper(componentModel = ComponentModel.JAKARTA_CDI)
public abstract class AuditEventMapper {
	@Inject
	ObjectMapper objectMapper;

	@Mapping(target = "sourceInfo", expression = "java(toAuditSource(llmInteractionCompleteEvent.sourceInfo()))")
	@Mapping(target = "result", expression = "java(toJson(llmInteractionCompleteEvent.result()))")
	public abstract LLMInteractionCompleteAuditEvent toAuditEvent(LLMInteractionCompleteEvent llmInteractionCompleteEvent);

	@Mapping(target = "sourceInfo", expression = "java(toAuditSource(llmInteractionFailureEvent.sourceInfo()))")
	@Mapping(target = "errorMessage", expression = "java(getMessage(llmInteractionFailureEvent.error()))")
	@Mapping(target = "causeErrorMessage", expression = "java(getCauseMessage(llmInteractionFailureEvent.error()))")
	public abstract LLMInteractionFailedAuditEvent toAuditEvent(LLMInteractionFailureEvent llmInteractionFailureEvent);

	@Mapping(target = "sourceInfo", expression = "java(toAuditSource(responseFromLLMReceivedEvent.sourceInfo()))")
	@Mapping(target = "response", expression = "java(responseFromLLMReceivedEvent.response().aiMessage().text())")
	@Mapping(target = "modelName", expression = "java(responseFromLLMReceivedEvent.response().modelName())")
	@Mapping(target = "inputTokenCount", expression = "java(responseFromLLMReceivedEvent.response().tokenUsage().inputTokenCount())")
	@Mapping(target = "outputTokenCount", expression = "java(responseFromLLMReceivedEvent.response().tokenUsage().outputTokenCount())")
	public abstract LLMResponseReceivedAuditEvent toAuditEvent(ResponseFromLLMReceivedEvent responseFromLLMReceivedEvent);

	@Mapping(target = "sourceInfo", expression = "java(toAuditSource(toolExecutedEvent.sourceInfo()))")
	@Mapping(target = "result", expression = "java(toolExecutedEvent.result())")
	@Mapping(target = "toolName", expression = "java(toolExecutedEvent.request().name())")
	@Mapping(target = "toolArgs", expression = "java(toolExecutedEvent.request().arguments())")
	public abstract ToolExecutedAuditEvent toAuditEvent(ToolExecutedEvent toolExecutedEvent);

	@Mapping(target = "sourceInfo", expression = "java(toAuditSource(inputGuardrailExecutedEvent.sourceInfo()))")
	@Mapping(target = "userMessage", expression = "java((inputGuardrailExecutedEvent.params().userMessage() != null) ? inputGuardrailExecutedEvent.params().userMessage().singleText() : null)")
	@Mapping(target = "rewrittenUserMessage", expression = "java((inputGuardrailExecutedEvent.rewrittenUserMessage() != null) ? inputGuardrailExecutedEvent.rewrittenUserMessage().singleText() : null)")
	@Mapping(target = "result", expression = "java(inputGuardrailExecutedEvent.result().result().name())")
	@Mapping(target = "guardrailClass", expression = "java(inputGuardrailExecutedEvent.guardrailClass().getName())")
	public abstract InputGuardrailExecutedAuditEvent toAuditEvent(InputGuardrailExecutedEvent inputGuardrailExecutedEvent);

	@Mapping(target = "sourceInfo", expression = "java(toAuditSource(outputGuardrailExecutedEvent.sourceInfo()))")
	@Mapping(target = "response", expression = "java((outputGuardrailExecutedEvent.params().responseFromLLM() != null) ? outputGuardrailExecutedEvent.params().responseFromLLM().text() : null)")
	@Mapping(target = "result", expression = "java(outputGuardrailExecutedEvent.result().result().name())")
	@Mapping(target = "guardrailClass", expression = "java(outputGuardrailExecutedEvent.guardrailClass().getName())")
	public abstract OutputGuardrailExecutedAuditEvent toAuditEvent(OutputGuardrailExecutedEvent outputGuardrailExecutedEvent);

	@Mapping(target = "interfaceName", expression = "java(auditSourceInfo.interfaceName())")
	@Mapping(target = "interactionId", expression = "java(auditSourceInfo.interactionId())")
	@Mapping(target = "methodName", expression = "java(auditSourceInfo.methodName())")
	public abstract AuditSource toAuditSource(AuditSourceInfo auditSourceInfo);

	protected Map<String, String> toJson(Object object) {
		return (object != null) ?
		       this.objectMapper.convertValue(object, new TypeReference<>() {}) :
		       null;
	}

	protected String getMessage(Throwable t) {
		return Optional.ofNullable(t)
			.map(Throwable::getMessage)
			.orElse(null);
	}

	protected String getCauseMessage(Throwable t) {
		return Optional.ofNullable(t)
			.map(Throwable::getCause)
			.map(this::getMessage)
			.orElse(null);
	}
}
