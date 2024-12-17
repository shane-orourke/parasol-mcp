package org.ericoleg.ndnp.ai.guardrail;

import jakarta.enterprise.context.ApplicationScoped;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrail;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

@ApplicationScoped
public class PolitenessOutputGuardrail implements OutputGuardrail {
	static final String REPROMPT_MESSAGE = "Invalid email";
	static final String REPROMPT_PROMPT = "The response was not polite and respectful. Please try again.";

	private final PolitenessService politenessService;

	public PolitenessOutputGuardrail(PolitenessService politenessService) {
		this.politenessService = politenessService;
	}

	@Override
	public OutputGuardrailResult validate(AiMessage responseFromLLM) {
		return this.politenessService.isPolite(responseFromLLM.text()) ?
		       success() :
		       reprompt(REPROMPT_MESSAGE, REPROMPT_PROMPT);
	}
}
