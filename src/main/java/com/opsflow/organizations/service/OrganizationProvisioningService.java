package com.opsflow.organizations.service;

import com.opsflow.common.tenancy.TenantContext;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.UUID;

@Service
public class OrganizationProvisioningService {

    private final OrganizationRepository organizationRepository;

    public OrganizationProvisioningService(OrganizationRepository organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    public Organization createOrganization(String name) {
        String baseSlug = generateSlug(name);
        DataIntegrityViolationException lastConflict = null;
        for (int attempt = 0; attempt < 25; attempt++) {
            String slug = attempt == 0 ? baseSlug : baseSlug + "-" + attempt;
            UUID id = UUID.randomUUID();
            TenantContext.setOrganizationId(id);
            Organization organization = new Organization(name.trim(), slug);
            organization.setId(id);
            try {
                return organizationRepository.save(organization);
            } catch (DataIntegrityViolationException ex) {
                lastConflict = ex;
            }
        }
        throw lastConflict != null
            ? lastConflict
            : new IllegalStateException("Could not allocate an organization slug");
    }

    String generateSlug(String name) {
        if (name == null || name.isBlank()) {
            return "org-" + UUID.randomUUID().toString().substring(0, 8);
        }
        String slug = name.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
        return slug.isBlank() ? "org-" + UUID.randomUUID().toString().substring(0, 8) : slug;
    }
}
