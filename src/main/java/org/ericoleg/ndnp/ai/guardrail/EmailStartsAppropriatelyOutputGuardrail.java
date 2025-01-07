package org.ericoleg.ndnp.ai.guardrail;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

import org.ericoleg.ndnp.ai.Email;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.AbstractJsonExtractorOutputGuardrail;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

@Priority(8)
@ApplicationScoped
public class EmailStartsAppropriatelyOutputGuardrail extends AbstractJsonExtractorOutputGuardrail {
	static final String REPROMPT_MESSAGE = "Invalid email body";
	static final String REPROMPT_PROMPT = "The email body did not start with 'Dear'. Please try again.";

	@Override
	public OutputGuardrailResult validate(AiMessage responseFromLLM) {
		var result = super.validate(responseFromLLM);

		if (result.isSuccess()) {
			var email = (Email) result.successfulResult();

			return email.body().startsWith("Dear ") ?
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
