package org.parasol.ai.guardrail;

import static io.quarkiverse.langchain4j.guardrails.GuardrailAssertions.assertThat;
import static org.assertj.core.api.Assertions.atIndex;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;

import org.parasol.ai.Email;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;

import dev.langchain4j.data.message.AiMessage;
import io.quarkiverse.langchain4j.guardrails.GuardrailResult.Result;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailParams;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult.Failure;

@QuarkusTest
class CompositeOutputGuardrailTests {
	private static final String SUBJECT = "This is a subject";
	private static final String SUBJECT2 = "This is a second subject";
	private static final String BODY = "Hello. Claim number 1234 has been approved.";
	private static final String BODY2 = "Hola. Claim number 5678 has been approved.";
	private static final String JSON = "{\"subject\":\"" + SUBJECT + "\",\"body\":\"" + BODY + "\"}";
	private static final String JSON2 = "{\"subject\":\"" + SUBJECT2 + "\",\"body\":\"" + BODY + "\"}";
	private static final Email EMAIL = new Email(SUBJECT, BODY);
	private static final Email EMAIL2 = new Email(SUBJECT2, BODY2);
	private static final AiMessage MESSAGE = AiMessage.from(JSON);
	private static final OutputGuardrailParams PARAMS = new OutputGuardrailParams(MESSAGE, null, null, null, Map.of());
	private static final OutputGuardrailResult REPROMPT_RESULT = new OutputGuardrailResult(Result.FATAL, null, null, List.of(new Failure("Guardrail failure", null, true, "You failed, try again")));
	private static final OutputGuardrailResult SUCCESS_WITH_RESULT = OutputGuardrailResult.successWith(JSON, EMAIL);
	private static final OutputGuardrailResult SUCCESS_WITH_RESULT2 = OutputGuardrailResult.successWith(JSON2, EMAIL2);

	@Inject
	CompositeOutputGuardrail compositeOutputGuardrail;

	@InjectMock
	EmailContainsRequiredInformationOutputGuardrail emailContainsRequiredInformationOutputGuardrail;

	@InjectMock
	EmailStartsAppropriatelyOutputGuardrail emailStartsAppropriatelyOutputGuardrail;

	@InjectMock
	EmailEndsAppropriatelyOutputGuardrail emailEndsAppropriatelyOutputGuardrail;

	@InjectMock
	PolitenessOutputGuardrail politenessOutputGuardrail;

	@BeforeEach
	void beforeEach() {
		doReturn(OutputGuardrailResult.success())
			.when(this.emailEndsAppropriatelyOutputGuardrail)
			.validate(PARAMS);

		doReturn(OutputGuardrailResult.success())
			.when(this.politenessOutputGuardrail)
			.validate(PARAMS);
	}

	@Test
	void hasCorrectGuardrails() {
		assertThat(this.compositeOutputGuardrail.getGuardrails())
			.isNotNull()
			.hasSize(4)
			.satisfies(guardrail -> assertThat(guardrail).isInstanceOf(EmailContainsRequiredInformationOutputGuardrail.class), atIndex(0))
			.satisfies(guardrail -> assertThat(guardrail).isInstanceOf(EmailStartsAppropriatelyOutputGuardrail.class), atIndex(1))
			.satisfies(guardrail -> assertThat(guardrail).isInstanceOf(EmailEndsAppropriatelyOutputGuardrail.class), atIndex(2))
			.satisfies(guardrail -> assertThat(guardrail).isInstanceOf(PolitenessOutputGuardrail.class), atIndex(3));
	}

	@Test
	void guardrailSuccess() {
		doReturn(OutputGuardrailResult.success())
			.when(this.emailContainsRequiredInformationOutputGuardrail)
			.validate(PARAMS);

		doReturn(OutputGuardrailResult.success())
			.when(this.emailStartsAppropriatelyOutputGuardrail)
			.validate(PARAMS);

		assertThat(this.compositeOutputGuardrail.validate(PARAMS))
			.isSuccessful();

		verify(this.emailContainsRequiredInformationOutputGuardrail).validate(PARAMS);
		verify(this.emailStartsAppropriatelyOutputGuardrail).validate(PARAMS);
		verify(this.emailEndsAppropriatelyOutputGuardrail).validate(PARAMS);
		verify(this.politenessOutputGuardrail).validate(PARAMS);
	}

	@Test
	void guardrailSuccessWhenReturningSuccessWithResultFirst() {
		doReturn(SUCCESS_WITH_RESULT)
			.when(this.emailContainsRequiredInformationOutputGuardrail)
			.validate(PARAMS);

		doReturn(OutputGuardrailResult.success())
			.when(this.emailStartsAppropriatelyOutputGuardrail)
			.validate(PARAMS);

		assertThat(this.compositeOutputGuardrail.validate(PARAMS))
			.isSuccessful()
			.extracting(
				OutputGuardrailResult::successfulText,
				OutputGuardrailResult::successfulResult
			)
			.containsExactly(
				JSON,
				EMAIL
			);

		verify(this.emailContainsRequiredInformationOutputGuardrail).validate(PARAMS);
		verify(this.emailStartsAppropriatelyOutputGuardrail).validate(PARAMS);
		verify(this.emailEndsAppropriatelyOutputGuardrail).validate(PARAMS);
		verify(this.politenessOutputGuardrail).validate(PARAMS);
	}

	@Test
	void guardrailSuccessWhenReturningSuccessWithResultDownstream() {
		doReturn(OutputGuardrailResult.success())
			.when(this.emailContainsRequiredInformationOutputGuardrail)
			.validate(PARAMS);

		doReturn(SUCCESS_WITH_RESULT)
			.when(this.emailStartsAppropriatelyOutputGuardrail)
			.validate(PARAMS);

		assertThat(this.compositeOutputGuardrail.validate(PARAMS))
			.isSuccessful()
			.extracting(
				OutputGuardrailResult::successfulText,
				OutputGuardrailResult::successfulResult
			)
			.containsExactly(
				JSON,
				EMAIL
			);

		verify(this.emailContainsRequiredInformationOutputGuardrail).validate(PARAMS);
		verify(this.emailStartsAppropriatelyOutputGuardrail).validate(PARAMS);
		verify(this.emailEndsAppropriatelyOutputGuardrail).validate(PARAMS);
		verify(this.politenessOutputGuardrail).validate(PARAMS);
	}

	@Test
	void guardrailSuccessWhenReturningMultipleSuccessWithResult() {
		doReturn(SUCCESS_WITH_RESULT)
			.when(this.emailContainsRequiredInformationOutputGuardrail)
			.validate(PARAMS);

		doReturn(SUCCESS_WITH_RESULT2)
			.when(this.emailStartsAppropriatelyOutputGuardrail)
			.validate(PARAMS);

		assertThat(this.compositeOutputGuardrail.validate(PARAMS))
			.isSuccessful()
			.extracting(
				OutputGuardrailResult::successfulText,
				OutputGuardrailResult::successfulResult
			)
			.containsExactly(
				JSON2,
				EMAIL2
			);

		verify(this.emailContainsRequiredInformationOutputGuardrail).validate(PARAMS);
		verify(this.emailStartsAppropriatelyOutputGuardrail).validate(PARAMS);
		verify(this.emailEndsAppropriatelyOutputGuardrail).validate(PARAMS);
		verify(this.politenessOutputGuardrail).validate(PARAMS);
	}

	@Test
	void guardrailSingleFailure() {
		doReturn(REPROMPT_RESULT)
			.when(this.emailContainsRequiredInformationOutputGuardrail)
			.validate(PARAMS);

		assertThat(this.compositeOutputGuardrail.validate(PARAMS))
			.hasSingleFailureWithMessageAndReprompt(REPROMPT_RESULT.failures().getFirst().message(), REPROMPT_RESULT.getReprompt());

		verify(this.emailContainsRequiredInformationOutputGuardrail).validate(PARAMS);
		verify(this.emailStartsAppropriatelyOutputGuardrail, never()).validate(any(OutputGuardrailParams.class));
		verify(this.emailEndsAppropriatelyOutputGuardrail, never()).validate(any(OutputGuardrailParams.class));
		verify(this.politenessOutputGuardrail, never()).validate(any(OutputGuardrailParams.class));
	}

	@Test
	void guardrailDownstreamFailure() {
		doReturn(SUCCESS_WITH_RESULT)
			.when(this.emailContainsRequiredInformationOutputGuardrail)
			.validate(PARAMS);

		doReturn(REPROMPT_RESULT)
			.when(this.emailStartsAppropriatelyOutputGuardrail)
			.validate(PARAMS);

		assertThat(this.compositeOutputGuardrail.validate(PARAMS))
			.hasSingleFailureWithMessageAndReprompt(REPROMPT_RESULT.failures().getFirst().message(), REPROMPT_RESULT.getReprompt());

		verify(this.emailContainsRequiredInformationOutputGuardrail).validate(PARAMS);
		verify(this.emailStartsAppropriatelyOutputGuardrail).validate(PARAMS);
		verify(this.emailEndsAppropriatelyOutputGuardrail, never()).validate(any(OutputGuardrailParams.class));
		verify(this.politenessOutputGuardrail, never()).validate(any(OutputGuardrailParams.class));
	}
}