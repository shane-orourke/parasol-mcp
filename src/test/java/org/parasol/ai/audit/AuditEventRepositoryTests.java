package org.parasol.ai.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.parasol.model.audit.AuditSource;
import org.parasol.model.audit.LLMInitialMessagesCreatedAuditEvent;
import org.parasol.model.audit.LLMInteractionCompleteAuditEvent;
import org.parasol.model.audit.LLMInteractionFailedAuditEvent;
import org.parasol.model.audit.LLMInteractions.LLMInteraction;

import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
class AuditEventRepositoryTests {
	private static final String EVENT_QUERY_TEMPLATE = "from AuditEvent e WHERE type(e) = %s AND sourceInfo.interactionId = ?1";

	@Inject
	AuditEventRepository repository;

	@Inject
	ObjectMapper objectMapper;

	private record SomeObject(String field1, int field2) {}

	@Test
	@TestTransaction
	@Order(0)
	void interactions() throws JsonProcessingException {
		// Set up some interactions
		var first = initialMessages();
		var firstComplete = interactionComplete(first);
		var second = initialMessages();
		var secondFailed = interactionFailed(second);
		this.repository.flush();

		// Get the interactions
		var interactions = this.repository.getLLMInteractions(Optional.empty(), Optional.empty());

		assertThat(interactions)
			.isNotNull();

		assertThat(interactions.interactions())
			.isNotNull()
			.hasSize(2)
			.containsExactlyInAnyOrder(
				new LLMInteraction(
					first.getSourceInfo().getInteractionId(),
					first.getCreatedOn(),
					first.getSystemMessage(),
					first.getUserMessage(),
					firstComplete.getResult(),
					null,
					null
				),
				new LLMInteraction(
					second.getSourceInfo().getInteractionId(),
					second.getCreatedOn(),
					second.getSystemMessage(),
					second.getUserMessage(),
					null,
					secondFailed.getErrorMessage(),
					secondFailed.getCauseErrorMessage()
				)
			);
	}

	@Test
	@TestTransaction
	@Order(1)
	void initialMessagesCreated() {
		initialMessages();
	}

	@Test
	@TestTransaction
	@Order(1)
	void interactionComplete() throws JsonProcessingException {
		interactionComplete(initialMessages());
	}

	@Test
	@TestTransaction
	@Order(1)
	void interactionFailed() {
		interactionFailed(initialMessages());
	}

	private LLMInteractionFailedAuditEvent interactionFailed(LLMInitialMessagesCreatedAuditEvent initialMessagesCreatedEvent) {
		var interactionFailedEvent = LLMInteractionFailedAuditEvent.builder()
		                                                           .errorMessage("Some error message")
		                                                           .causeErrorMessage("Some cause error message")
		                                                           .sourceInfo(initialMessagesCreatedEvent.getSourceInfo())
		                                                           .build();

		this.repository.persist(interactionFailedEvent);
		var interactionFailedEvents = this.repository.find(
			                                  EVENT_QUERY_TEMPLATE.formatted(LLMInteractionFailedAuditEvent.class.getSimpleName()),
			                                  interactionFailedEvent.getSourceInfo().getInteractionId()
		                                  )
		                                             .list();

		assertThat(interactionFailedEvents)
			.singleElement()
			.usingRecursiveComparison()
			.isEqualTo(interactionFailedEvent);

		return interactionFailedEvent;
	}

	private LLMInteractionCompleteAuditEvent interactionComplete(LLMInitialMessagesCreatedAuditEvent initialMessagesCreatedEvent) throws JsonProcessingException {
		var someObj = new AuditEventRepositoryTests.SomeObject("field1", 1);
		var interactionCompleteEvent = LLMInteractionCompleteAuditEvent.builder()
		                                                               .result(this.objectMapper.writeValueAsString(someObj))
		                                                               .sourceInfo(initialMessagesCreatedEvent.getSourceInfo())
		                                                               .build();

		this.repository.persist(interactionCompleteEvent);
		var interactionCompleteEvents = this.repository.find(EVENT_QUERY_TEMPLATE.formatted(LLMInteractionCompleteAuditEvent.class.getSimpleName()), interactionCompleteEvent.getSourceInfo()
		                                                                                                                                                                     .getInteractionId())
		                                               .list();

		assertThat(interactionCompleteEvents).singleElement()
		                                     .usingRecursiveComparison()
		                                     .isEqualTo(interactionCompleteEvent);

		assertThat(interactionCompleteEvents.getFirst()).isNotNull()
		                                                .isInstanceOf(LLMInteractionCompleteAuditEvent.class)
		                                                .extracting(e -> {
			                                                try {
				                                                return this.objectMapper.readValue(((LLMInteractionCompleteAuditEvent) e).getResult(), SomeObject.class);
			                                                }
			                                                catch (JsonProcessingException ex) {
				                                                throw new RuntimeException(ex);
			                                                }
		                                                })
		                                                .usingRecursiveComparison()
		                                                .isEqualTo(someObj);

		return interactionCompleteEvent;
	}

	private LLMInitialMessagesCreatedAuditEvent initialMessages() {
		var initialMessagesCreatedEvent = LLMInitialMessagesCreatedAuditEvent.builder()
		                                                                     .systemMessage("System message")
		                                                                     .userMessage("User message")
		                                                                     .sourceInfo(AuditSource.builder()
		                                                                                            .interactionId(UUID.randomUUID())
		                                                                                            .interfaceName("someInterface")
		                                                                                            .methodName("someMethod")
		                                                                                            .build())
		                                                                     .build();

		this.repository.persist(initialMessagesCreatedEvent);
		var initialMessagesCreatedEvents = this.repository.find(EVENT_QUERY_TEMPLATE.formatted(LLMInitialMessagesCreatedAuditEvent.class.getSimpleName()), initialMessagesCreatedEvent.getSourceInfo()
		                                                                                                                                                                              .getInteractionId())
		                                                  .list();

		assertThat(initialMessagesCreatedEvents).singleElement()
		                                        .usingRecursiveComparison()
		                                        .isEqualTo(initialMessagesCreatedEvent);

		return initialMessagesCreatedEvent;
	}
}