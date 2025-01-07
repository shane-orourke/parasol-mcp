package org.ericoleg.ndnp.ai.guardrail;

import io.quarkiverse.langchain4j.guardrails.OutputGuardrail;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailParams;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

public interface OutputGuardrailRule extends OutputGuardrail {
	@Override
	default OutputGuardrailResult validate(OutputGuardrailParams params) {
		return OutputGuardrail.super.validate(params);
	}
}
