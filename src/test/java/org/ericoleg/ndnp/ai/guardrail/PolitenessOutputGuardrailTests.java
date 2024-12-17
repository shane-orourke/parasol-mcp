package org.ericoleg.ndnp.ai.guardrail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.GuardrailResult;
import io.quarkiverse.langchain4j.guardrails.GuardrailResult.Result;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

class PolitenessOutputGuardrailTests {
	PolitenessService politenessService = mock(PolitenessService.class);
	PolitenessOutputGuardrail guardrail = spy(new PolitenessOutputGuardrail(politenessService));

	@Test
	void guardrailSuccess() {
		var aiMessage = AiMessage.from("This is a polite response");
		when(this.politenessService.isPolite(aiMessage.text()))
			.thenReturn(true);

		assertThat(this.guardrail.validate(aiMessage))
			.isNotNull()
			.isEqualTo(OutputGuardrailResult.success());

		verify(this.guardrail).validate(aiMessage);
		verify(this.guardrail).success();
		verify(this.politenessService).isPolite(aiMessage.text());
		verifyNoMoreInteractions(this.guardrail, this.politenessService);
	}

	@Test
	void emailIsntPolite() {
		var aiMessage = AiMessage.from("Hello. Claim number 1234 has been approved.");

		when(this.politenessService.isPolite(aiMessage.text()))
			.thenReturn(false);

		var guardrailResult = this.guardrail.validate(aiMessage);

		assertThat(guardrailResult.result())
			.isEqualTo(Result.FATAL);

		assertThat(guardrailResult.failures())
			.asInstanceOf(InstanceOfAssertFactories.list(GuardrailResult.Failure.class))
			.singleElement()
			.extracting(GuardrailResult.Failure::message)
			.isEqualTo(PolitenessOutputGuardrail.REPROMPT_MESSAGE);

		verify(this.guardrail).validate(aiMessage);
		verify(this.guardrail).reprompt(PolitenessOutputGuardrail.REPROMPT_MESSAGE, PolitenessOutputGuardrail.REPROMPT_PROMPT);
		verify(this.politenessService).isPolite(aiMessage.text());
		verifyNoMoreInteractions(this.guardrail, this.politenessService);
	}
}