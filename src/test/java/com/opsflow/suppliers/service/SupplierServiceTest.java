package com.opsflow.suppliers.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.suppliers.domain.Supplier;
import com.opsflow.suppliers.dto.CreateSupplierRequest;
import com.opsflow.suppliers.dto.SupplierResponse;
import com.opsflow.suppliers.dto.UpdateSupplierRequest;
import com.opsflow.suppliers.repository.SupplierRepository;
import com.opsflow.users.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Organization organization;
    private UserPrincipal adminPrincipal;
    private UUID orgId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        organization = new Organization("Acme Corp", "acme-corp");
        organization.setId(orgId);

        adminPrincipal = new UserPrincipal(
            UUID.randomUUID(),
            orgId,
            "admin@acme.com",
            "hashedPass",
            Role.ORG_ADMIN,
            true
        );
    }

    @Test
    void shouldCreateSupplierSuccessfully() {
        CreateSupplierRequest request = new CreateSupplierRequest(
            "Global Parts Ltd",
            "Alice Smith",
            "alice@globalparts.com",
            "+1234567890",
            "123 Industrial Park",
            "TAX-12345"
        );

        when(supplierRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, "Global Parts Ltd")).thenReturn(false);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        Supplier saved = new Supplier(
            organization,
            "Global Parts Ltd",
            "Alice Smith",
            "alice@globalparts.com",
            "+1234567890",
            "123 Industrial Park",
            "TAX-12345"
        );
        saved.setId(UUID.randomUUID());

        when(supplierRepository.save(any(Supplier.class))).thenReturn(saved);

        SupplierResponse response = supplierService.createSupplier(adminPrincipal, request);

        assertNotNull(response);
        assertEquals("Global Parts Ltd", response.name());
        assertEquals("alice@globalparts.com", response.email());
        verify(supplierRepository).save(any(Supplier.class));
    }

    @Test
    void shouldThrowExceptionWhenSupplierNameAlreadyExists() {
        CreateSupplierRequest request = new CreateSupplierRequest(
            "Duplicate Supplier",
            "Bob",
            "bob@dup.com",
            null,
            null,
            null
        );

        when(supplierRepository.existsByOrganizationIdAndNameIgnoreCase(orgId, "Duplicate Supplier")).thenReturn(true);

        assertThrows(BusinessException.class, () -> supplierService.createSupplier(adminPrincipal, request));
        verify(supplierRepository, never()).save(any(Supplier.class));
    }

    @Test
    void shouldSoftDeleteSupplier() {
        UUID supplierId = UUID.randomUUID();
        Supplier supplier = new Supplier(organization, "Old Supplier", null, null, null, null, null);
        supplier.setId(supplierId);
        supplier.setActive(true);

        when(supplierRepository.findByIdAndOrganizationIdAndActiveTrue(supplierId, orgId)).thenReturn(Optional.of(supplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(inv -> inv.getArgument(0));

        SupplierResponse response = supplierService.deleteSupplier(adminPrincipal, supplierId);

        assertFalse(response.active());
        verify(supplierRepository).save(supplier);
    }
}
