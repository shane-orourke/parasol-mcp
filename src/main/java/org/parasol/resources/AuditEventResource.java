package org.parasol.resources;

import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.parasol.ai.audit.AuditEventRepository;
import org.parasol.model.audit.AuditEvent;

import io.quarkus.panache.common.Sort;

@Path("/auditEvents")
@Produces(MediaType.APPLICATION_JSON)
public class AuditEventResource {
	private final AuditEventRepository auditEventRepository;

	public AuditEventResource(AuditEventRepository auditEventRepository) {
		this.auditEventRepository = auditEventRepository;
	}

	@GET
	public List<AuditEvent> getAllAuditEvents() {
		return this.auditEventRepository.listAll(Sort.by("createdOn"));
	}

	@GET
	@Path("/{interactionId}")
	public List<AuditEvent> getEventsForInteraction(@PathParam("interactionId") UUID interactionId) {
		return this.auditEventRepository.getAllForInteractionId(interactionId);
	}
}
