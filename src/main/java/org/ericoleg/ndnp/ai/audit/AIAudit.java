package org.ericoleg.ndnp.ai.audit;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.quarkus.logging.Log;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import io.quarkiverse.langchain4j.audit.Audit;

public class AIAudit extends Audit {
	private final String id;
	private final String method;

	public AIAudit(CreateInfo createInfo) {
		super(createInfo);

		this.id = UUID.randomUUID().toString();
		this.method = "%s#%s".formatted(createInfo.interfaceName(), createInfo.methodName());
	}

	@Override
	public void initialMessages(Optional<SystemMessage> systemMessage, UserMessage userMessage) {
		Log.infof("[%s - %s] Initial messages: systemMessage=%s, userMessage=%s", this.method, this.id, systemMessage.map(SystemMessage::text).orElse(""), userMessage);
	}

	@Override
	public void addRelevantDocument(List<TextSegment> segments, UserMessage userMessage) {
		Log.infof("[%s - %s] Document added to message being sent to LLM: userMessage=%s, segments=%s", this.method, this.id, userMessage, segments);
	}

	@Override
	public void addLLMToApplicationMessage(Response<AiMessage> response) {
		Log.infof("[%s - %s] LLM to application message: response=%s", this.method, this.id, response);
	}

	@Override
	public void addApplicationToLLMMessage(ToolExecutionResultMessage toolExecutionResultMessage) {
		Log.infof("[%s - %s] application to LLM message: toolExecutionResultMessage=%s", this.method, this.id, toolExecutionResultMessage);
	}

	@Override
	public void onCompletion(Object result) {
		Log.infof("[%s - %s] Completion: result=%s", this.method, this.id, result);
	}

	@Override
	public void onFailure(Exception e) {
		Log.errorf(e, "[%s - %s] Failure: %s", this.method, this.id, e.getMessage());
	}
}
