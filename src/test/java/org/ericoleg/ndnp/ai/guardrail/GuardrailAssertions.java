package org.ericoleg.ndnp.ai.guardrail;

import org.assertj.core.api.Assertions;

import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

/**
 * Custom assertions for working with Guardrails
 * <p>
 *   <strong>NOTE:</strong> This will eventually be contributed upstream to quarkus-langchain4j.
 *   Just keeping it here for now as a working prototype. See https://github.com/quarkiverse/quarkus-langchain4j/pull/1174
 * </p>
 */
public class GuardrailAssertions extends Assertions {
	public static OutputGuardrailResultAssert assertThat(OutputGuardrailResult actual) {
		return OutputGuardrailResultAssert.assertThat(actual);
	}
}
