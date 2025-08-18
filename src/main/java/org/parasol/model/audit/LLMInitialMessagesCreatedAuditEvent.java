package org.parasol.model.audit;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("INITIAL_MESSAGES_CREATED")
public class LLMInitialMessagesCreatedAuditEvent extends AuditEvent {
	@Column(updatable = false, columnDefinition = "TEXT")
	private String systemMessage;
	
	@Column(updatable = false, columnDefinition = "TEXT")
	private String userMessage;

	// JPA requires a no-arg constructor with at least protected visibility
	protected LLMInitialMessagesCreatedAuditEvent() {
		super();
	}

	// Private constructor used by the builder
	private LLMInitialMessagesCreatedAuditEvent(Builder builder) {
		super(builder);
		this.systemMessage = builder.systemMessage;
		this.userMessage = builder.userMessage;
	}
	
	@Override
	public AuditEventType getEventType() {
		return AuditEventType.INITIAL_MESSAGES_CREATED;
	}

	public String getSystemMessage() {
		return systemMessage;
	}

	public void setSystemMessage(String systemMessage) {
		this.systemMessage = systemMessage;
	}

	public String getUserMessage() {
		return userMessage;
	}

	public void setUserMessage(String userMessage) {
		this.userMessage = userMessage;
	}

	public static Builder builder() {
		return new Builder();
	}

	public Builder toBuilder() {
		return new Builder(this);
	}

	@Override
	public String toString() {
		return "LLMInitialMessagesCreatedAuditEvent{" +
			"systemMessage='" + getSystemMessage() + '\'' +
			", userMessage='" + getUserMessage() + '\'' +
			", id=" + getId() +
			", sourceInfo=" + getSourceInfo() +
			'}';
	}

	public static final class Builder extends AuditEvent.Builder<Builder, LLMInitialMessagesCreatedAuditEvent> {
		private String systemMessage;
		private String userMessage;

		private Builder() {
			super();
		}

		private Builder(LLMInitialMessagesCreatedAuditEvent source) {
			super(source);
			this.systemMessage = source.systemMessage;
			this.userMessage = source.userMessage;
		}

		public Builder systemMessage(String systemMessage) {
			this.systemMessage = systemMessage;
			return this;
		}

		public Builder userMessage(String userMessage) {
			this.userMessage = userMessage;
			return this;
		}

		@Override
		public LLMInitialMessagesCreatedAuditEvent build() {
			return new LLMInitialMessagesCreatedAuditEvent(this);
		}
	}
}
