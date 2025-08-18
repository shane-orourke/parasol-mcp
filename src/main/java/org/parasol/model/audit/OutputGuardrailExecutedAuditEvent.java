package org.parasol.model.audit;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("OUTPUT_GUARDRAIL_EXECUTED")
public class OutputGuardrailExecutedAuditEvent extends AuditEvent {
	@Column(updatable = false, columnDefinition = "TEXT")
	private String response;

	@Column(updatable = false, columnDefinition = "TEXT")
	private String guardrailResult;

	@Column(updatable = false)
	private String guardrailClass;

	// JPA requires a public or protected no-arg constructor
	protected OutputGuardrailExecutedAuditEvent() {
		super();
	}

	// Private constructor used by the builder
	private OutputGuardrailExecutedAuditEvent(Builder builder) {
		super(builder);
		this.response = builder.response;
		this.guardrailResult = builder.result;
		this.guardrailClass = builder.guardrailClass;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public AuditEventType getEventType() {
		return AuditEventType.OUTPUT_GUARDRAIL_EXECUTED;
	}

	public String getGuardrailClass() {
		return guardrailClass;
	}

	public void setGuardrailClass(String guardrailClass) {
		this.guardrailClass = guardrailClass;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public String getGuardrailResult() {
		return guardrailResult;
	}

	public void setGuardrailResult(String result) {
		this.guardrailResult = result;
	}

	@Override
	public String toString() {
		return "OutputGuardrailExecutedAuditEvent{" +
			"guardrailClass='" + getGuardrailClass() + '\'' +
			", response='" + getResponse() + '\'' +
			", guardrailResult='" + getGuardrailResult() + '\'' +
			", id=" + getId() +
			", sourceInfo=" + getSourceInfo() +
			'}';
	}

	public static final class Builder extends AuditEvent.Builder<Builder, OutputGuardrailExecutedAuditEvent> {
		private String response;
		private String result;
		private String guardrailClass;

		private Builder() {
			super();
		}

		private Builder(OutputGuardrailExecutedAuditEvent source) {
			super(source);
			this.response = source.response;
			this.result = source.guardrailResult;
			this.guardrailClass = source.guardrailClass;
		}

		public Builder response(String response) {
			this.response = response;
			return this;
		}

		public Builder result(String result) {
			this.result = result;
			return this;
		}

		public Builder guardrailClass(String guardrailClass) {
			this.guardrailClass = guardrailClass;
			return this;
		}

		public OutputGuardrailExecutedAuditEvent build() {
			return new OutputGuardrailExecutedAuditEvent(this);
		}
	}
}
