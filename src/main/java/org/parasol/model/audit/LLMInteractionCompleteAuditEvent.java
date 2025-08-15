package org.parasol.model.audit;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@DiscriminatorValue("LLM_INTERACTION_COMPLETE")
public class LLMInteractionCompleteAuditEvent extends AuditEvent {
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(updatable = false)
	private Map<String, String> result = new HashMap<>();

	protected LLMInteractionCompleteAuditEvent() {
		super();
	}

	private LLMInteractionCompleteAuditEvent(Builder builder) {
		super(builder);
		this.result.putAll(builder.result);
	}

	@Override
	public AuditEventType getEventType() {
		return AuditEventType.LLM_INTERACTION_COMPLETE;
	}

	public Map<String, String> getResult() {
		return result;
	}

	public void setResult(Map<String, String> result) {
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
			"result='" + getResult() + '\'' +
			", eventType=" + getEventType() +
			", id=" + getId() +
			", sourceInfo=" + getSourceInfo() + '}';
	}

	public static final class Builder extends AuditEvent.Builder<Builder, LLMInteractionCompleteAuditEvent> {
		private Map<String, String> result = new HashMap<>();

		private Builder() {
			super();
		}

		private Builder(LLMInteractionCompleteAuditEvent source) {
			super(source);
			this.result = source.result;
		}

		public Builder result(Map<String, String> result) {
			if (result != null) {
				this.result.putAll(result);
			}

			return this;
		}

		@Override
		public LLMInteractionCompleteAuditEvent build() {
			return new LLMInteractionCompleteAuditEvent(this);
		}
	}
}
