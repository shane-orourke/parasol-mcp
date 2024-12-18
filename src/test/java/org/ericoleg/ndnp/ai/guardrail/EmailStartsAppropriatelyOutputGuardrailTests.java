package org.ericoleg.ndnp.ai.guardrail;

import static org.ericoleg.ndnp.ai.guardrail.GuardrailAssertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.GuardrailResult.Result;

class EmailStartsAppropriatelyOutputGuardrailTests {
	EmailStartsAppropriatelyOutputGuardrail guardrail = spy(new EmailStartsAppropriatelyOutputGuardrail());

	@Test
	void guardrailSuccess() {
		var aiMessage = AiMessage.from("Dear John,");

		assertThat(this.guardrail.validate(aiMessage))
			.isSuccessful();

		verify(this.guardrail).validate(aiMessage);
		verify(this.guardrail).success();
		verifyNoMoreInteractions(this.guardrail);
	}

	@Test
	void emailDoesntStartAppropriately() {
		var aiMessage = AiMessage.from("Hello there.");
		var guardrailResult = this.guardrail.validate(aiMessage);

		assertThat(guardrailResult)
			.hasResult(Result.FATAL)
			.hasSingleFailureWithMessage(EmailStartsAppropriatelyOutputGuardrail.REPROMPT_MESSAGE);

		verify(this.guardrail).validate(aiMessage);
		verify(this.guardrail).reprompt(EmailStartsAppropriatelyOutputGuardrail.REPROMPT_MESSAGE, EmailStartsAppropriatelyOutputGuardrail.REPROMPT_PROMPT);
		verifyNoMoreInteractions(this.guardrail);
	}
}