package org.parasol.model.audit;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("LLM_INTERACTION_FAILED")
public class LLMInteractionFailedAuditEvent extends AuditEvent {
	@Column(updatable = false, columnDefinition = "TEXT")
	private String errorMessage;

	@Column(updatable = false, columnDefinition = "TEXT")
	private String causeErrorMessage;

	protected LLMInteractionFailedAuditEvent() {
		// Required by JPA
	}

	private LLMInteractionFailedAuditEvent(Builder builder) {
		super(builder);
		this.errorMessage = builder.errorMessage;
		this.causeErrorMessage = builder.causeErrorMessage;
	}

	@Override
	public AuditEventType getEventType() {
		return AuditEventType.LLM_INTERACTION_FAILED;
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	public String getCauseErrorMessage() {
		return causeErrorMessage;
	}

	public void setCauseErrorMessage(String causeErrorMessage) {
		this.causeErrorMessage = causeErrorMessage;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	@Override
	public String toString() {
		return "LLMInteractionFailedAuditEvent{" +
			"eventType='" + getEventType() + '\'' +
			", causeErrorMessage='" + getCauseErrorMessage() + '\'' +
			", errorMessage='" + getErrorMessage() + '\'' +
			", id=" + getId() +
			", sourceInfo=" + getSourceInfo() +
			'}';
	}

	public static final class Builder extends AuditEvent.Builder<Builder, LLMInteractionFailedAuditEvent> {
		private String errorMessage;
		private String causeErrorMessage;

		private Builder() {
			super();
		}

		private Builder(LLMInteractionFailedAuditEvent source) {
			super(source);
			this.errorMessage = source.errorMessage;
			this.causeErrorMessage = source.causeErrorMessage;
		}

		public Builder errorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
			return this;
		}

		public Builder causeErrorMessage(String causeErrorMessage) {
			this.causeErrorMessage = causeErrorMessage;
			return this;
		}

		@Override
		public LLMInteractionFailedAuditEvent build() {
			return new LLMInteractionFailedAuditEvent(this);
		}
	}
}
