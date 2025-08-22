package org.parasol.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import org.parasol.ai.audit.AuditObserved;
import org.parasol.mapping.AuditEventMapper;
import org.parasol.model.audit.AuditDates;
import org.parasol.model.audit.AuditEvent;
import org.parasol.model.audit.AuditStats;
import org.parasol.model.audit.AuditStats.InteractionStats;
import org.parasol.model.audit.LLMInteractions;
import org.parasol.model.audit.LLMInteractions.LLMInteraction;

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
	private static final String STATS_NATIVE_QUERY = """
		WITH per_interaction AS (
      SELECT
        interaction_id,
        MIN(created_on) AS interaction_date,
        COUNT(*) FILTER (WHERE event_type = 'LLM_INTERACTION_FAILED') AS num_llm_failures,
        COUNT(*) FILTER (WHERE event_type = 'OUTPUT_GUARDRAIL_EXECUTED') AS total_output_guardrail_executions,
        COUNT(*) FILTER (WHERE event_type = 'OUTPUT_GUARDRAIL_EXECUTED' AND guardrail_result IN ('FATAL', 'FAILURE')) AS total_output_guardrail_failures
      FROM audit_events 
      GROUP BY interaction_id
		)
		SELECT
      interaction_id,
      interaction_date,
		  num_llm_failures,
		  total_output_guardrail_executions,
		  total_output_guardrail_failures,
		  CASE
		    WHEN total_output_guardrail_executions > 0
		      THEN AVG(total_output_guardrail_executions) FILTER (WHERE total_output_guardrail_failures > 0) OVER ()
		    ELSE 0
		  END AS avg_output_guardrail_executions,
			CASE
		    WHEN total_output_guardrail_failures > 0
		      THEN AVG(total_output_guardrail_failures) FILTER (WHERE total_output_guardrail_failures > 0) OVER ()
		    ELSE 0
		  END AS avg_output_guardrail_failures
		FROM per_interaction
		WHERE interaction_date BETWEEN :start_date AND :end_date
		ORDER BY interaction_date
		""";

	private static final String INTERACTIONS_NATIVE_QUERY = """
		WITH per_interaction AS (
			SELECT
				interaction_id,
				MIN(created_on) AS interaction_date,
				MAX(system_message) FILTER (WHERE event_type = 'INITIAL_MESSAGES_CREATED') AS system_message,
				MAX(user_message) FILTER (WHERE event_type = 'INITIAL_MESSAGES_CREATED') AS user_message,
				MAX(result) FILTER (WHERE event_type = 'LLM_INTERACTION_COMPLETE') AS result,
				MAX(error_message) FILTER (WHERE event_type = 'LLM_INTERACTION_FAILED') AS error_message,
				MAX(cause_error_message) FILTER (WHERE event_type = 'LLM_INTERACTION_FAILED') AS cause_error_message
			FROM audit_events
			GROUP BY interaction_id
		)
		SELECT
			interaction_id,
			interaction_date,
			system_message,
			user_message,
			result,
			error_message,
			cause_error_message
		FROM per_interaction
		WHERE interaction_date BETWEEN :start_date AND :end_date
		ORDER BY interaction_date
		""";

	private final AuditEventMapper auditEventMapper;

	public AuditEventRepository(AuditEventMapper auditEventMapper) {
		this.auditEventMapper = auditEventMapper;
	}

	public List<AuditEvent> getAllForInteractionId(UUID interactionId) {
		return find("sourceInfo.interactionId", Sort.by("createdOn"), interactionId).list();
	}

	public AuditStats getAuditStats(Optional<Instant> start, Optional<Instant> end) {
		var auditDates = AuditDates.from(start, end);
		var stats = getEntityManager().createNativeQuery(STATS_NATIVE_QUERY, InteractionStats.class)
			                  .setParameter("start_date", auditDates.start())
			                  .setParameter("end_date", auditDates.end())
			                  .getResultList();

		return new AuditStats(auditDates, stats);
	}

	public LLMInteractions getLLMInteractions(Optional<Instant> start, Optional<Instant> end) {
		var auditDates = AuditDates.from(start, end);
		var interactions = getEntityManager().createNativeQuery(INTERACTIONS_NATIVE_QUERY, LLMInteraction.class)
			.setParameter("start_date", auditDates.start())
			                  .setParameter("end_date", auditDates.end())
			                  .getResultList();

		return new LLMInteractions(auditDates, interactions);
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
