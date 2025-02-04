package org.parasol.ai.guardrail;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkus.arc.All;
import io.quarkus.arc.InstanceHandle;

import io.quarkiverse.langchain4j.guardrails.OutputGuardrail;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailParams;
import io.quarkiverse.langchain4j.guardrails.OutputGuardrailResult;

/**
 * This class should (and eventually will) move to the Quarkus LangChain4J project.
 */
@ApplicationScoped
public class CompositeOutputGuardrail implements OutputGuardrail {
	private final List<OutputGuardrail> guardrails;

	public CompositeOutputGuardrail(@All List<InstanceHandle<OutputGuardrail>> guardrails) {
		this.guardrails = Optional.ofNullable(guardrails)
			.orElseGet(List::of)
			.stream()
			.filter(instanceHandle -> !CompositeOutputGuardrail.class.equals(instanceHandle.getBean().getBeanClass()))
			.map(InstanceHandle::get)
			.toList();
	}

	@Override
	public OutputGuardrailResult validate(OutputGuardrailParams params) {
		OutputGuardrailResult result = null;

		for (var guardrail : this.guardrails) {
			// Get this guardrail's result
			var intermediateResult = guardrail.validate(params);

			if (!intermediateResult.isSuccess()) {
				// This guardrail failed, so return its result
				return intermediateResult;
			}

			if ((result == null) || intermediateResult.hasRewrittenResult()) {
				// This is the first guardrail or the guardrail has rewritten the result
				// Store this result
				result = intermediateResult;
			}
		}

		// Return the last result, or success if there weren't any guardrails
		return (result != null) ? result : success();
	}

	protected List<OutputGuardrail> getGuardrails() {
		return List.copyOf(this.guardrails);
	}
}
