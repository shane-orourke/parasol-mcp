package org.parasol.ai.guardrail;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;

import org.parasol.ai.Email;
import org.parasol.ai.GenerateEmailService;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

@Priority(6)
@ApplicationScoped
public class EmailEndsAppropriatelyOutputGuardrail extends GenerateEmailOutputGuardrail {
	static final String REPROMPT_MESSAGE = "Invalid email body";
	static final String REPROMPT_PROMPT = """
		The email body did not end properly. Please try again.
		
		The email body should end with the following text, EXACTLY as it appears below:
		""" + GenerateEmailService.EMAIL_ENDING;

	@Override
	public OutputGuardrailResult validate(AiMessage responseFromLLM) {
		var result = super.validate(responseFromLLM);

		if (result.isSuccess()) {
			var email = (Email) result.successfulResult();

			return email.body().endsWith(GenerateEmailService.EMAIL_ENDING) ?
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
