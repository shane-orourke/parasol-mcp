package org.ericoleg.ndnp.ai.audit;

import jakarta.enterprise.context.ApplicationScoped;

import io.quarkiverse.langchain4j.audit.Audit;
import io.quarkiverse.langchain4j.audit.Audit.CreateInfo;
import io.quarkiverse.langchain4j.audit.AuditService;

@ApplicationScoped
public class AiAuditService implements AuditService {
	@Override
	public Audit create(CreateInfo createInfo) {
		return new AIAudit(createInfo);
	}

	@Override
	public void complete(Audit audit) {
		// Do something with the audit - maybe store in a DB?
	}
}
