package org.parasol.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;

import org.parasol.mapping.AuditEventMapper;
import org.parasol.model.audit.AuditEvent;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.logging.Log;

import io.opentelemetry.instrumentation.annotations.SpanAttribute;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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

	@Transactional
	@WithSpan("Audit.llmInteractionComplete")
  public void llmInteractionComplete(@Observes @SpanAttribute("arg.event") LLMInteractionCompleteEvent e) {
    Log.infof(
			"LLM interaction complete:\nsource: %s\nresult: %s",
	    e.sourceInfo(),
	    e.result()
    );

		persist(this.auditEventMapper.toAuditEvent(e));
  }

	@Transactional
	@WithSpan("Audit.llmInteractionFailed")
  public void llmInteractionFailed(@Observes @SpanAttribute("arg.event") LLMInteractionFailureEvent e) {
    Log.infof(
			"LLM interaction failed:\nsource: %s\nfailure: %s",
	    e.sourceInfo(),
	    e.error().getMessage()
    );

		persist(this.auditEventMapper.toAuditEvent(e));
  }

	@Transactional
	@WithSpan("Audit.responseFromLLMReceived")
  public void responseFromLLMReceived(@Observes @SpanAttribute("arg.event") ResponseFromLLMReceivedEvent e) {
    Log.infof(
			"Response from LLM received:\nsource: %s\nresponse: %s",
	    e.sourceInfo(),
			e.response().aiMessage().text()
    );

		persist(this.auditEventMapper.toAuditEvent(e));
  }

	@Transactional
	@WithSpan("Audit.toolExecuted")
  public void toolExecuted(@Observes @SpanAttribute("arg.event") ToolExecutedEvent e) {
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
	@WithSpan("Audit.inputGuardrailExecuted")
	public void inputGuardrailExecuted(@Observes @SpanAttribute("arg.event") InputGuardrailExecutedEvent e) {
		Log.infof(
			"Input guardrail executed:\nuserMessage: %s\nresult: %s",
			e.rewrittenUserMessage().singleText(),
			e.result().result()
		);

		persist(this.auditEventMapper.toAuditEvent(e));
	}

	@Transactional
	@WithSpan("Audit.outputGuardrailExecuted")
	public void outputGuardrailExecuted(@Observes @SpanAttribute("arg.event") OutputGuardrailExecutedEvent e) {
		Log.infof(
			"Output guardrail executed:\nresponseFromLLM:%s\nresult: %s",
			e.params().responseFromLLM().text(),
			e.result().result()
		);

		persist(this.auditEventMapper.toAuditEvent(e));
	}
}
