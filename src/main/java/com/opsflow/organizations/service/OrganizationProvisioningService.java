package com.opsflow.organizations.service;

import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
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
        String slug = baseSlug;
        int counter = 1;
        while (organizationRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter++;
        }
        Organization organization = new Organization(name.trim(), slug);
        return organizationRepository.save(organization);
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
