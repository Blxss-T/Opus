package com.opsflow.audit.domain;

import com.opsflow.common.domain.BaseEntity;
import com.opsflow.organizations.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    /** User email, or "api-key:<name>" for an API key. */
    @Column(name = "actor", nullable = false)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_type", nullable = false, length = 20)
    private AuditActorType actorType;

    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    @Column(name = "resource_type", nullable = false, length = 50)
    private String resourceType;

    @Column(name = "resource_id")
    private UUID resourceId;

    @Column(name = "status_code", nullable = false)
    private int statusCode;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    public AuditEvent() {
    }

    public AuditEvent(
        Organization organization,
        UUID actorId,
        String actor,
        AuditActorType actorType,
        String httpMethod,
        String path,
        String resourceType,
        UUID resourceId,
        int statusCode,
        String ipAddress
    ) {
        this.organization = organization;
        this.actorId = actorId;
        this.actor = actor;
        this.actorType = actorType;
        this.httpMethod = httpMethod;
        this.path = path;
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.statusCode = statusCode;
        this.ipAddress = ipAddress;
    }

    public Organization getOrganization() {
        return organization;
    }

    public UUID getActorId() {
        return actorId;
    }

    public String getActor() {
        return actor;
    }

    public AuditActorType getActorType() {
        return actorType;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getResourceType() {
        return resourceType;
    }

    public UUID getResourceId() {
        return resourceId;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getIpAddress() {
        return ipAddress;
    }
}
