package com.opsflow.audit.dto;

import com.opsflow.audit.domain.AuditActorType;
import com.opsflow.audit.domain.AuditEvent;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID id,
    Instant occurredAt,
    UUID actorId,
    String actor,
    AuditActorType actorType,
    String method,
    String path,
    String resourceType,
    UUID resourceId,
    int statusCode,
    String ipAddress
) {
    public static AuditEventResponse fromEntity(AuditEvent event) {
        return new AuditEventResponse(
            event.getId(),
            event.getCreatedAt(),
            event.getActorId(),
            event.getActor(),
            event.getActorType(),
            event.getHttpMethod(),
            event.getPath(),
            event.getResourceType(),
            event.getResourceId(),
            event.getStatusCode(),
            event.getIpAddress()
        );
    }
}
