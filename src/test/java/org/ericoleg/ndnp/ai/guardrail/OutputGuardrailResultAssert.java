package org.ericoleg.ndnp.ai.guardrail;

import java.util.Objects;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ListAssert;

import io.quarkiverse.langchain4j.guardrails.GuardrailResult.Failure;
import io.quarkiverse.langchain4j.guardrails.GuardrailResult.Result;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

/**
 * Custom assertions for {@link OutputGuardrailResult}s
 * <p>
 *   <strong>NOTE:</strong> This will eventually be contributed upstream to quarkus-langchain4j.
 *   Just keeping it here for now as a working prototype. See https://github.com/quarkiverse/quarkus-langchain4j/pull/1174
 * </p>
 */
public class OutputGuardrailResultAssert extends AbstractObjectAssert<OutputGuardrailResultAssert, OutputGuardrailResult> {
	protected OutputGuardrailResultAssert(OutputGuardrailResult outputGuardrailResult) {
		super(outputGuardrailResult, OutputGuardrailResultAssert.class);
	}

	public static OutputGuardrailResultAssert assertThat(OutputGuardrailResult actual) {
		return new OutputGuardrailResultAssert(actual);
	}

	/**
	 * Asserts that the actual object's {@link Result} matches the given expected result.
	 * If the result does not match, an assertion error is thrown with the actual and expected values.
	 *
	 * @param result the expected result to compare against the actual object's result
	 * @return this assertion object for method chaining
	 * @throws AssertionError if the actual result does not match the expected result
	 */
	public OutputGuardrailResultAssert hasResult(Result result) {
		isNotNull();

		if (!Objects.equals(actual.result(), result)) {
			throw failureWithActualExpected(actual.result(), result, "Expected result to be <%s> but was <%s>", result, actual.result());
		}

		return this;
	}

	/**
	 * Asserts that the actual {@code OutputGuardrailResult} represents a successful state.
	 * A successful state is determined by having a {@link Result} of {@link Result#SUCCESS}
	 * and being equal to {@link OutputGuardrailResult#success()}.
	 *
	 * @return this assertion object for method chaining
	 * @throws AssertionError if the actual result is not successful as per the aforementioned criteria
	 */
	public OutputGuardrailResultAssert isSuccessful() {
		isNotNull();
		hasResult(Result.SUCCESS);
		isEqualTo(OutputGuardrailResult.success());

		return this;
	}

	/**
	 * Asserts that the actual {@code OutputGuardrailResult} contains failures.
	 * The method validates that the object being asserted is not null and
	 * that there are failures present within the result.
	 *
	 * @return this assertion object for method chaining
	 * @throws AssertionError if the actual object is null or if the failures are empty
	 */
	public OutputGuardrailResultAssert hasFailures() {
		isNotNull();
		getFailuresAssert().isNotEmpty();

		return this;
	}

	/**
	 * Asserts that the actual {@code OutputGuardrailResult} contains exactly one failure with the specified message.
	 * If the assertion fails, an error is thrown detailing the problem.
	 *
	 * @param expectedFailureMessage the expected message of the single failure
	 * @return this assertion object for method chaining
	 * @throws AssertionError if the actual object is null, if there are no failures,
	 *                        if there is more than one failure, or if the single failure
	 *                        does not match the specified message
	 */
	public OutputGuardrailResultAssert hasSingleFailureWithMessage(String expectedFailureMessage) {
		isNotNull();

		getFailuresAssert()
			.singleElement()
			.extracting(Failure::message)
			.isEqualTo(expectedFailureMessage);

		return this;
	}

	private ListAssert<Failure> getFailuresAssert() {
		return Assertions.assertThat(actual.failures())
			.isNotNull()
			.asInstanceOf(InstanceOfAssertFactories.list(Failure.class));
	}
}
