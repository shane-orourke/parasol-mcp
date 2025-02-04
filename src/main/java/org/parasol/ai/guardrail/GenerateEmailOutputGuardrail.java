package org.parasol.ai.guardrail;

import org.parasol.ai.GenerateEmailService;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.AbstractJsonExtractorOutputGuardrail;

public abstract class GenerateEmailOutputGuardrail extends AbstractJsonExtractorOutputGuardrail {
	@Override
	protected String getInvalidJsonMessage(AiMessage aiMessage, String json) {
		return "Invalid JSON format in response";
	}

	@Override
	protected String getInvalidJsonReprompt(AiMessage aiMessage, String json) {
		return GenerateEmailService.JSON_STRUCTURE;
	}
}
