package org.ericoleg.ndnp.ai.guardrail;

import jakarta.enterprise.context.ApplicationScoped;

import org.ericoleg.ndnp.ai.GenerateEmailService;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrail;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

@ApplicationScoped
public class EmailEndsAppropriatelyOutputGuardrail implements OutputGuardrail {
	static final String REPROMPT_MESSAGE = "Invalid email";
	static final String REPROMPT_PROMPT = """
		The response did not end properly. Please try again.
		
		The email body should end with the following text, EXACTLY as it appears below:
		""" + GenerateEmailService.EMAIL_ENDING;

	@Override
	public OutputGuardrailResult validate(AiMessage responseFromLLM) {
		return responseFromLLM.text().endsWith(GenerateEmailService.EMAIL_ENDING) ?
		       success() :
		       reprompt(REPROMPT_MESSAGE, REPROMPT_PROMPT);
	}
}
