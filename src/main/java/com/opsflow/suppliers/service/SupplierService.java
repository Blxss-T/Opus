package com.opsflow.suppliers.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.common.jpa.FilterSpecs;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.suppliers.domain.Supplier;
import com.opsflow.suppliers.dto.CreateSupplierRequest;
import com.opsflow.suppliers.dto.SupplierResponse;
import com.opsflow.suppliers.dto.UpdateSupplierRequest;
import com.opsflow.suppliers.repository.SupplierRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final OrganizationRepository organizationRepository;

    public SupplierService(
        SupplierRepository supplierRepository,
        OrganizationRepository organizationRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public SupplierResponse createSupplier(UserPrincipal principal, CreateSupplierRequest request) {
        UUID organizationId = principal.getOrganizationId();
        String normalizedName = request.name().trim();

        if (supplierRepository.existsByOrganizationIdAndNameIgnoreCase(organizationId, normalizedName)) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                String.format("Supplier with name '%s' already exists in your organization", normalizedName)
            );
        }

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        Supplier supplier = new Supplier(
            organization,
            normalizedName,
            request.contactPerson() != null ? request.contactPerson().trim() : null,
            request.email() != null ? request.email().trim().toLowerCase() : null,
            request.phone() != null ? request.phone().trim() : null,
            request.address() != null ? request.address().trim() : null,
            request.taxNumber() != null ? request.taxNumber().trim() : null
        );

        Supplier saved = supplierRepository.save(supplier);
        return SupplierResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(UserPrincipal principal, UUID supplierId) {
        Supplier supplier = findSupplierForTenant(principal.getOrganizationId(), supplierId);
        return SupplierResponse.fromEntity(supplier);
    }

    @Transactional(readOnly = true)
    public Page<SupplierResponse> listSuppliers(UserPrincipal principal, String q, Pageable pageable) {
        Specification<Supplier> spec = FilterSpecs.and(
            FilterSpecs.organizationIs(principal.getOrganizationId()),
            FilterSpecs.equalsValue("active", true),
            FilterSpecs.ilikeAny(q, "name", "email", "contactPerson")
        );
        Pageable effective = FilterSpecs.withDefaultSort(pageable, Sort.Order.asc("name"));
        return supplierRepository.findAll(spec, effective).map(SupplierResponse::fromEntity);
    }

    @Transactional
    public SupplierResponse updateSupplier(UserPrincipal principal, UUID supplierId, UpdateSupplierRequest request) {
        UUID organizationId = principal.getOrganizationId();
        Supplier supplier = findSupplierForTenant(organizationId, supplierId);
        String normalizedName = request.name().trim();

        if (supplierRepository.existsByOrganizationIdAndNameIgnoreCaseAndIdNot(organizationId, normalizedName, supplierId)) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                String.format("Another supplier with name '%s' already exists in your organization", normalizedName)
            );
        }

        supplier.setName(normalizedName);
        supplier.setContactPerson(request.contactPerson() != null ? request.contactPerson().trim() : null);
        supplier.setEmail(request.email() != null ? request.email().trim().toLowerCase() : null);
        supplier.setPhone(request.phone() != null ? request.phone().trim() : null);
        supplier.setAddress(request.address() != null ? request.address().trim() : null);
        supplier.setTaxNumber(request.taxNumber() != null ? request.taxNumber().trim() : null);

        Supplier saved = supplierRepository.save(supplier);
        return SupplierResponse.fromEntity(saved);
    }

    @Transactional
    public SupplierResponse deleteSupplier(UserPrincipal principal, UUID supplierId) {
        Supplier supplier = findSupplierForTenant(principal.getOrganizationId(), supplierId);
        supplier.setActive(false);
        Supplier saved = supplierRepository.save(supplier);
        return SupplierResponse.fromEntity(saved);
    }

    private Supplier findSupplierForTenant(UUID organizationId, UUID supplierId) {
        return supplierRepository.findByIdAndOrganizationIdAndActiveTrue(supplierId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Supplier", "id", supplierId));
    }
}
