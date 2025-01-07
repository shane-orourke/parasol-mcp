package org.ericoleg.ndnp.ai.guardrail;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

import org.ericoleg.ndnp.ai.Email;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.AbstractJsonExtractorOutputGuardrail;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

@Priority(4)
@ApplicationScoped
public class PolitenessOutputGuardrail extends AbstractJsonExtractorOutputGuardrail {
	static final String REPROMPT_MESSAGE = "Invalid email";
	static final String REPROMPT_PROMPT = "The response was not polite and respectful. Please try again.";

	private final PolitenessService politenessService;

	public PolitenessOutputGuardrail(PolitenessService politenessService) {
		this.politenessService = politenessService;
	}

	@Override
	public OutputGuardrailResult validate(AiMessage responseFromLLM) {
		var result = super.validate(responseFromLLM);

		if (result.isSuccess()) {
			var email = (Email) result.successfulResult();

			return this.politenessService.isPolite(email.body()) ?
			       result :
			       reprompt(REPROMPT_MESSAGE, REPROMPT_PROMPT);
		}

		return result;
	}

	@Override
	protected Class<?> getOutputClass() {
		return Email.class;
	}
}
