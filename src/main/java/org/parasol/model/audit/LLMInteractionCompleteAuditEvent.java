package org.parasol.model.audit;


import static org.parasol.model.audit.LLMInteractionCompleteAuditEvent.EVENT_TYPE;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue(EVENT_TYPE)
public class LLMInteractionCompleteAuditEvent extends AuditEvent {
	public static final String EVENT_TYPE = "LLM_INTERACTION_COMPLETE";

	@Column(updatable = false, columnDefinition = "TEXT")
	private String result;

	protected LLMInteractionCompleteAuditEvent() {
		super();
	}

	private LLMInteractionCompleteAuditEvent(Builder builder) {
		super(builder);
		this.result = builder.result;
	}

	@Override
	public AuditEventType getEventType() {
		return AuditEventType.LLM_INTERACTION_COMPLETE;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	public static Builder builder() {
		return new Builder();
	}

	@Override
	public String toString() {
		return "LLMInteractionCompleteAuditEvent{" +
			"eventType='" + getEventType() + '\'' +
			", result='" + getResult() + '\'' +
			", id=" + getId() +
			", sourceInfo=" + getSourceInfo() + '}';
	}

	public static final class Builder extends AuditEvent.Builder<Builder, LLMInteractionCompleteAuditEvent> {
		private String result;

		private Builder() {
			super();
		}

		private Builder(LLMInteractionCompleteAuditEvent source) {
			super(source);
			this.result = source.result;
		}

		public Builder result(String result) {
			this.result = result;
			return this;
		}

		@Override
		public LLMInteractionCompleteAuditEvent build() {
			return new LLMInteractionCompleteAuditEvent(this);
		}
	}
}
