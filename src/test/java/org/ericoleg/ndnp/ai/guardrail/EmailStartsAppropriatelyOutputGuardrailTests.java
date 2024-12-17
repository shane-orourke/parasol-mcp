package org.ericoleg.ndnp.ai.guardrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.GuardrailResult;
import io.quarkiverse.langchain4j.guardrails.GuardrailResult.Result;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

class EmailStartsAppropriatelyOutputGuardrailTests {
	EmailStartsAppropriatelyOutputGuardrail guardrail = spy(new EmailStartsAppropriatelyOutputGuardrail());

	@Test
	void guardrailSuccess() {
		var aiMessage = AiMessage.from("Dear John,");

		assertThat(this.guardrail.validate(aiMessage))
			.isNotNull()
			.isEqualTo(OutputGuardrailResult.success());

		verify(this.guardrail).validate(aiMessage);
		verify(this.guardrail).success();
		verifyNoMoreInteractions(this.guardrail);
	}

	@Test
	void emailDoesntStartAppropriately() {
		var aiMessage = AiMessage.from("Hello there.");
		var guardrailResult = this.guardrail.validate(aiMessage);

		assertThat(guardrailResult.result())
			.isEqualTo(Result.FATAL);

		assertThat(guardrailResult.failures())
			.asInstanceOf(InstanceOfAssertFactories.list(GuardrailResult.Failure.class))
			.singleElement()
			.extracting(GuardrailResult.Failure::message)
			.isEqualTo(EmailStartsAppropriatelyOutputGuardrail.REPROMPT_MESSAGE);

		verify(this.guardrail).validate(aiMessage);
		verify(this.guardrail).reprompt(EmailStartsAppropriatelyOutputGuardrail.REPROMPT_MESSAGE, EmailStartsAppropriatelyOutputGuardrail.REPROMPT_PROMPT);
		verifyNoMoreInteractions(this.guardrail);
	}
}