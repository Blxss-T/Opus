package com.opsflow.audit.service;

import com.opsflow.apikeys.security.ApiKeyAuthenticationFilter;
import com.opsflow.audit.domain.AuditActorType;
import com.opsflow.audit.domain.AuditEvent;
import com.opsflow.audit.dto.AuditEventResponse;
import com.opsflow.audit.repository.AuditEventRepository;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.jpa.FilterSpecs;
import com.opsflow.organizations.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuditService {

    private static final int MAX_PATH_LENGTH = 500;

    private final AuditEventRepository auditEventRepository;
    private final OrganizationRepository organizationRepository;

    public AuditService(AuditEventRepository auditEventRepository, OrganizationRepository organizationRepository) {
        this.auditEventRepository = auditEventRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public void record(
        UserPrincipal principal,
        String httpMethod,
        String path,
        String resourceType,
        UUID resourceId,
        int statusCode,
        String ipAddress
    ) {
        boolean apiKey = principal.getUsername().startsWith(ApiKeyAuthenticationFilter.ACTOR_PREFIX);
        auditEventRepository.save(new AuditEvent(
            organizationRepository.getReferenceById(principal.getOrganizationId()),
            principal.getId(),
            principal.getUsername(),
            apiKey ? AuditActorType.API_KEY : AuditActorType.USER,
            httpMethod,
            path.length() > MAX_PATH_LENGTH ? path.substring(0, MAX_PATH_LENGTH) : path,
            resourceType,
            resourceId,
            statusCode,
            ipAddress
        ));
    }

    @Transactional(readOnly = true)
    public Page<AuditEventResponse> list(
        UserPrincipal principal,
        String actor,
        AuditActorType actorType,
        String resourceType,
        UUID resourceId,
        Pageable pageable
    ) {
        Specification<AuditEvent> spec = FilterSpecs.and(
            FilterSpecs.organizationIs(principal.getOrganizationId()),
            FilterSpecs.ilikeAny(actor, "actor"),
            FilterSpecs.equalsValue("actorType", actorType),
            FilterSpecs.equalsIgnoreCase("resourceType", resourceType),
            FilterSpecs.equalsValue("resourceId", resourceId)
        );
        Pageable effective = FilterSpecs.withDefaultSort(pageable, Sort.Order.desc("createdAt"));
        return auditEventRepository.findAll(spec, effective).map(AuditEventResponse::fromEntity);
    }
}
