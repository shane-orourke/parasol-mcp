package org.parasol.ai.audit;

import java.util.List;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import org.parasol.mapping.AuditEventMapper;
import org.parasol.model.audit.AuditEvent;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.logging.Log;
import io.quarkus.panache.common.Sort;

import dev.langchain4j.data.message.SystemMessage;
import io.quarkiverse.langchain4j.audit.InitialMessagesCreatedEvent;
import io.quarkiverse.langchain4j.audit.InputGuardrailExecutedEvent;
import io.quarkiverse.langchain4j.audit.LLMInteractionCompleteEvent;
import io.quarkiverse.langchain4j.audit.LLMInteractionFailureEvent;
import io.quarkiverse.langchain4j.audit.OutputGuardrailExecutedEvent;
import io.quarkiverse.langchain4j.audit.ResponseFromLLMReceivedEvent;
import io.quarkiverse.langchain4j.audit.ToolExecutedEvent;

@ApplicationScoped
public class AuditEventRepository implements PanacheRepository<AuditEvent> {
	private final AuditEventMapper auditEventMapper;

	public AuditEventRepository(AuditEventMapper auditEventMapper) {
		this.auditEventMapper = auditEventMapper;
	}

	public List<AuditEvent> getAllForInteractionId(UUID interactionId) {
		return find("sourceInfo.interactionId", Sort.by("createdOn"), interactionId).list();
	}

	@Transactional
	@AuditObserved(
		name = "parasol.llm.initialmessages.created",
		description = "A count of LLM initial messages created",
		unit = "messages created"
	)
	public void llmInitialMessagesCreated(@Observes InitialMessagesCreatedEvent e) {
		Log.infof(
			"LLM initial messages created:\nsource: %s\nsystemMessage: %s\nuserMessage: %s",
			e.sourceInfo(),
			e.systemMessage().map(SystemMessage::text).orElse(""),
			e.userMessage().singleText()
		);

		persist(this.auditEventMapper.toAuditEvent(e));
	}

	@Transactional
	@AuditObserved(
		name = "parasol.llm.interaction.completed",
		description = "A count of LLM interactions completed",
		unit = "completed interactions"
	)
	public void llmInteractionComplete(@Observes LLMInteractionCompleteEvent e) {
		Log.infof(
			"LLM interaction complete:\nsource: %s\nresult: %s",
			e.sourceInfo(),
			e.result()
		);

		persist(this.auditEventMapper.toAuditEvent(e));
	}

	@Transactional
	@AuditObserved(
		name = "parasol.llm.interaction.failed",
		description = "A count of LLM interactions failed",
		unit = "failed interactions"
	)
	public void llmInteractionFailed(@Observes LLMInteractionFailureEvent e) {
		Log.infof(
			"LLM interaction failed:\nsource: %s\nfailure: %s",
			e.sourceInfo(),
			e.error().getMessage()
		);

		persist(this.auditEventMapper.toAuditEvent(e));
	}

	@Transactional
	@AuditObserved(
		name = "parasol.llm.response.received",
		description = "A count of LLM responses received",
		unit = "received responses"
	)
	public void responseFromLLMReceived(@Observes ResponseFromLLMReceivedEvent e) {
		Log.infof(
			"Response from LLM received:\nsource: %s\nresponse: %s",
			e.sourceInfo(),
			e.response().aiMessage().text()
		);

		persist(this.auditEventMapper.toAuditEvent(e));
	}

	@Transactional
	@AuditObserved(
		name = "parasol.llm.tool.executed",
		description = "A count of tools executed",
		unit = "executed tools"
	)
	public void toolExecuted(@Observes ToolExecutedEvent e) {
		Log.infof(
			"Tool executed:\nsource: %s\nrequest: %s(%s)\nresult: %s",
			e.sourceInfo(),
			e.request().name(),
			e.request().arguments(),
			e.result()
		);

		persist(this.auditEventMapper.toAuditEvent(e));
	}

	@Transactional
	@AuditObserved(
		name = "parasol.llm.guardrail.input.executed",
		description = "A count of input guardrails executed",
		unit = "executed input guardrails"
	)
	public void inputGuardrailExecuted(@Observes InputGuardrailExecutedEvent e) {
		Log.infof(
			"Input guardrail executed:\nuserMessage: %s\nresult: %s",
			e.rewrittenUserMessage().singleText(),
			e.result().result()
		);

		persist(this.auditEventMapper.toAuditEvent(e));
	}

	@Transactional
	@AuditObserved(
		name = "parasol.llm.guardrail.output.executed",
		description = "A count of output guardrails executed",
		unit = "executed output guardrails"
	)
	public void outputGuardrailExecuted(@Observes OutputGuardrailExecutedEvent e) {
		Log.infof("Output guardrail executed:\nresponseFromLLM:%s\nresult: %s",
			e.params().responseFromLLM().text(),
			e.result().result()
		);

		persist(this.auditEventMapper.toAuditEvent(e));
	}
}
