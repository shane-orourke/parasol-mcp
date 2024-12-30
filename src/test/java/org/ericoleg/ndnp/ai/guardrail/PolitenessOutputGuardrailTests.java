package org.ericoleg.ndnp.ai.guardrail;

import static io.quarkiverse.langchain4j.guardrails.GuardrailAssertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.GuardrailResult.Result;

class PolitenessOutputGuardrailTests {
	PolitenessService politenessService = mock(PolitenessService.class);
	PolitenessOutputGuardrail guardrail = spy(new PolitenessOutputGuardrail(politenessService));

	@Test
	void guardrailSuccess() {
		var aiMessage = AiMessage.from("This is a polite response");
		when(this.politenessService.isPolite(aiMessage.text()))
			.thenReturn(true);

		assertThat(this.guardrail.validate(aiMessage))
			.isSuccessful();

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

		assertThat(guardrailResult)
			.hasResult(Result.FATAL)
			.hasSingleFailureWithMessage(PolitenessOutputGuardrail.REPROMPT_MESSAGE);

		verify(this.guardrail).validate(aiMessage);
		verify(this.guardrail).reprompt(PolitenessOutputGuardrail.REPROMPT_MESSAGE, PolitenessOutputGuardrail.REPROMPT_PROMPT);
		verify(this.politenessService).isPolite(aiMessage.text());
		verifyNoMoreInteractions(this.guardrail, this.politenessService);
	}
}