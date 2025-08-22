package org.parasol.model.audit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import io.quarkus.runtime.annotations.RegisterForReflection;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

@RegisterForReflection
public record LLMInteractions(AuditDates auditDates, List<LLMInteraction> interactions) {
	public static LLMInteractions empty(AuditDates auditDates) {
		return new LLMInteractions(auditDates, List.of());
	}

	public record LLMInteraction(
		UUID interactionId,
		Instant interactionDate,
		String systemMessage,
		String userMessage,
		String result,
		String errorMessage,
		String causeErrorMessage
	) {
		public enum InteractionStatus { SUCCESS, FAILURE, UNKNOWN }

		@JsonProperty(value = "status", access = Access.READ_ONLY)
		public InteractionStatus getStatus() {
			return Optional.ofNullable(errorMessage)
				.map(String::strip)
				.filter(e -> !e.isEmpty())
				.map(e -> InteractionStatus.FAILURE)
				.orElse(InteractionStatus.SUCCESS);
		}

		@JsonProperty(value = "isFailure", access = Access.READ_ONLY)
		public boolean isFailure() {
			return getStatus() == InteractionStatus.FAILURE;
		}

		@JsonProperty(value = "isSuccess", access = Access.READ_ONLY)
		public boolean isSuccess() {
			return getStatus() == InteractionStatus.SUCCESS;
		}
	}
}
