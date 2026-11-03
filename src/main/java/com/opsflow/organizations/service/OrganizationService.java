package com.opsflow.organizations.service;

import com.opsflow.auth.dto.OrganizationResponse;
import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.dto.UpdateOrganizationRequest;
import com.opsflow.organizations.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrganizationService {

    private final OrganizationRepository organizationRepository;

    public OrganizationService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @Transactional(readOnly = true)
    public OrganizationResponse getCurrent(UserPrincipal principal) {
        return OrganizationResponse.fromEntity(findForCaller(principal));
    }

    /** Renames the organization. The slug is a stable identifier and does not change. */
    @Transactional
    public OrganizationResponse updateCurrent(UserPrincipal principal, UpdateOrganizationRequest request) {
        Organization organization = findForCaller(principal);
        organization.setName(request.name().trim());
        return OrganizationResponse.fromEntity(organizationRepository.save(organization));
    }

    private Organization findForCaller(UserPrincipal principal) {
        UUID organizationId = principal.getOrganizationId();
        return organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));
    }
}
