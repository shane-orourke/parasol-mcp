package org.parasol.mapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.parasol.model.audit.AuditEventType;
import org.parasol.model.audit.AuditSource;
import org.parasol.model.audit.InputGuardrailExecutedAuditEvent;
import org.parasol.model.audit.LLMInteractionCompleteAuditEvent;
import org.parasol.model.audit.LLMInteractionFailedAuditEvent;
import org.parasol.model.audit.LLMResponseReceivedAuditEvent;
import org.parasol.model.audit.OutputGuardrailExecutedAuditEvent;
import org.parasol.model.audit.ToolExecutedAuditEvent;

import io.quarkus.test.junit.QuarkusTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.output.TokenUsage;
import io.quarkiverse.langchain4j.audit.AuditSourceInfo;
import io.quarkiverse.langchain4j.audit.internal.DefaultInputGuardrailExecutedEvent;
import io.quarkiverse.langchain4j.audit.internal.DefaultLLMInteractionCompleteEvent;
import io.quarkiverse.langchain4j.audit.internal.DefaultLLMInteractionFailureEvent;
import io.quarkiverse.langchain4j.audit.internal.DefaultOutputGuardrailExecutedEvent;
import io.quarkiverse.langchain4j.audit.internal.DefaultResponseFromLLMReceivedEvent;
import io.quarkiverse.langchain4j.audit.internal.DefaultToolExecutedEvent;
import io.quarkiverse.langchain4j.guardrails.InputGuardrail;
import io.quarkiverse.langchain4j.guardrails.InputGuardrailParams;
import io.quarkiverse.langchain4j.guardrails.InputGuardrailResult;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrail;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailParams;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;
import io.quarkiverse.langchain4j.runtime.aiservice.NoopChatMemory;

@QuarkusTest
class AuditEventMapperTests {
	private static final UUID INTERACTION_ID = UUID.randomUUID();
	private static final SomeObject SOME_OBJECT = new SomeObject("field1", 2);
	private static final Exception SOME_ERROR = new RuntimeException("Some error", new NullPointerException("null pointer!"));
	private static final ChatResponse CHAT_RESPONSE = ChatResponse.builder()
		.modelName("someModel")
		.aiMessage(AiMessage.from("Some response"))
		.tokenUsage(new TokenUsage(2, 5, 7))
		.build();

	private static final ToolExecutionRequest TOOL_EXECUTION_REQUEST = ToolExecutionRequest.builder()
		.id("1")
		.name("doSomething")
		.arguments("1")
		.build();

	private static final InputGuardrailParams INPUT_GUARDRAIL_PARAMS = new InputGuardrailParams(
		UserMessage.from("do something"),
		new NoopChatMemory(),
		null,
		"do something",
		Map.of()
	);

	private static final OutputGuardrailParams OUTPUT_GUARDRAIL_PARAMS = new OutputGuardrailParams(
		AiMessage.from("Some response"),
		new NoopChatMemory(),
		null,
		"do something",
		Map.of()
	);

	@Inject
	AuditEventMapper auditEventMapper;

	@Inject
	ObjectMapper objectMapper;

	private AuditSourceInfo auditSourceInfo = new MockAuditSourceInfo();

	@Test
	void mapsLLMInteractionComplete() throws JsonProcessingException {
		var event = new DefaultLLMInteractionCompleteEvent(this.auditSourceInfo, SOME_OBJECT);
		var auditEvent = this.auditEventMapper.toAuditEvent(event);

		assertThat(auditEvent)
			.isNotNull()
			.extracting(LLMInteractionCompleteAuditEvent::getEventType)
			.isEqualTo(AuditEventType.LLM_INTERACTION_COMPLETE);

		checkAuditSource(auditEvent.getSourceInfo());
		assertThat(this.objectMapper.readValue(auditEvent.getResult(), new TypeReference<Map<String, Object>>() {}))
			.hasSize(2)
			.containsOnly(
				entry("field1", SOME_OBJECT.field1()),
				entry("field2", SOME_OBJECT.field2())
			);
	}

	@Test
	void mapsLLMInteractionFailedAuditEvent() {
		var event = new DefaultLLMInteractionFailureEvent(this.auditSourceInfo, SOME_ERROR);
		var auditEvent = this.auditEventMapper.toAuditEvent(event);

		assertThat(auditEvent)
			.isNotNull()
			.extracting(
				LLMInteractionFailedAuditEvent::getEventType,
				LLMInteractionFailedAuditEvent::getErrorMessage,
				LLMInteractionFailedAuditEvent::getCauseErrorMessage
			)
			.containsExactly(
				AuditEventType.LLM_INTERACTION_FAILED,
				SOME_ERROR.getMessage(),
				SOME_ERROR.getCause().getMessage()
			);

		checkAuditSource(auditEvent.getSourceInfo());
	}

	@Test
	void mapsLLMResponseReceivedAuditEvent() {
		var event = new DefaultResponseFromLLMReceivedEvent(this.auditSourceInfo, CHAT_RESPONSE);
		var auditEvent = this.auditEventMapper.toAuditEvent(event);

		assertThat(auditEvent)
			.isNotNull()
			.extracting(
				LLMResponseReceivedAuditEvent::getEventType,
				LLMResponseReceivedAuditEvent::getResponse,
				LLMResponseReceivedAuditEvent::getModelName,
				LLMResponseReceivedAuditEvent::getInputTokenCount,
				LLMResponseReceivedAuditEvent::getOutputTokenCount,
				LLMResponseReceivedAuditEvent::getTokenCount
			)
			.containsExactly(
				AuditEventType.LLM_RESPONSE_RECEIVED,
				CHAT_RESPONSE.aiMessage().text(),
				CHAT_RESPONSE.modelName(),
				CHAT_RESPONSE.tokenUsage().inputTokenCount(),
				CHAT_RESPONSE.tokenUsage().outputTokenCount(),
				CHAT_RESPONSE.tokenUsage().totalTokenCount()
			);

		checkAuditSource(auditEvent.getSourceInfo());
	}

	@Test
	void mapsToolExecutedAuditEvent() {
		var event = new DefaultToolExecutedEvent(this.auditSourceInfo, TOOL_EXECUTION_REQUEST, "result");
		var auditEvent = this.auditEventMapper.toAuditEvent(event);

		assertThat(auditEvent)
			.isNotNull()
			.extracting(
				ToolExecutedAuditEvent::getEventType,
				ToolExecutedAuditEvent::getToolName,
				ToolExecutedAuditEvent::getToolArgs,
				ToolExecutedAuditEvent::getToolResult
			)
			.containsExactly(
				AuditEventType.TOOL_EXECUTED,
				TOOL_EXECUTION_REQUEST.name(),
				TOOL_EXECUTION_REQUEST.arguments(),
				event.result()
			);

		checkAuditSource(auditEvent.getSourceInfo());
	}

	@Test
	void mapsInputGuardrailExecutedAuditEvent() {
		InputGuardrail guardrail = new MyInputGuardrail();
		var result = guardrail.validate(INPUT_GUARDRAIL_PARAMS.userMessage());
		var event = new DefaultInputGuardrailExecutedEvent(this.auditSourceInfo, INPUT_GUARDRAIL_PARAMS, result, (Class<InputGuardrail>) guardrail.getClass());
		var auditEvent = this.auditEventMapper.toAuditEvent(event);

		assertThat(auditEvent)
			.isNotNull()
			.extracting(
				InputGuardrailExecutedAuditEvent::getEventType,
				InputGuardrailExecutedAuditEvent::getUserMessage,
				InputGuardrailExecutedAuditEvent::getRewrittenUserMessage,
				InputGuardrailExecutedAuditEvent::getResult,
				InputGuardrailExecutedAuditEvent::getGuardrailClass
			)
			.containsExactly(
				AuditEventType.INPUT_GUARDRAIL_EXECUTED,
				INPUT_GUARDRAIL_PARAMS.userMessage().singleText(),
				"new text",
				result.result().name(),
				guardrail.getClass().getName()
			);

		checkAuditSource(auditEvent.getSourceInfo());
	}

	@Test
	void mapsOutputGuardrailExecutedAuditEvent() {
		OutputGuardrail guardrail = new MyOutputGuardrail();
		var result = guardrail.validate(OUTPUT_GUARDRAIL_PARAMS);
		var event = new DefaultOutputGuardrailExecutedEvent(this.auditSourceInfo, OUTPUT_GUARDRAIL_PARAMS, result, (Class<OutputGuardrail>) guardrail.getClass());
		var auditEvent = this.auditEventMapper.toAuditEvent(event);

		assertThat(auditEvent)
			.isNotNull()
			.extracting(
				OutputGuardrailExecutedAuditEvent::getEventType,
				OutputGuardrailExecutedAuditEvent::getResponse,
				OutputGuardrailExecutedAuditEvent::getGuardrailResult,
				OutputGuardrailExecutedAuditEvent::getGuardrailClass
			)
			.containsExactly(
				AuditEventType.OUTPUT_GUARDRAIL_EXECUTED,
				OUTPUT_GUARDRAIL_PARAMS.responseFromLLM().text(),
				result.result().name(),
				guardrail.getClass().getName()
			);

		checkAuditSource(auditEvent.getSourceInfo());
	}

	private static void checkAuditSource(AuditSource auditSource) {
		assertThat(auditSource)
			.isNotNull()
			.extracting(
				AuditSource::getInteractionId,
				AuditSource::getInterfaceName,
				AuditSource::getMethodName
			)
			.containsExactly(
				INTERACTION_ID,
				"someInterface",
				"someMethod"
			);
	}

	private record SomeObject(String field1, int field2) {}

	private static class MockAuditSourceInfo implements AuditSourceInfo {
		@Override
		public String interfaceName() {
			return "someInterface";
		}

		@Override
		public String methodName() {
			return "someMethod";
		}

		@Override
		public Optional<Integer> memoryIDParamPosition() {
			return Optional.empty();
		}

		@Override
		public Object[] methodParams() {
			return new Object[0];
		}

		@Override
		public UUID interactionId() {
			return INTERACTION_ID;
		}
	}

	private static class MyInputGuardrail implements InputGuardrail {
		@Override
		public InputGuardrailResult validate(UserMessage userMessage) {
			return successWith("new text");
		}
	}

	private static class MyOutputGuardrail implements OutputGuardrail {
		@Override
		public OutputGuardrailResult validate(AiMessage responseFromLLM) {
			return successWith("new text", "new result");
		}
	}
}