package com.opsflow.customers.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.customers.domain.Customer;
import com.opsflow.customers.dto.CreateCustomerRequest;
import com.opsflow.customers.dto.CustomerResponse;
import com.opsflow.customers.repository.CustomerRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.users.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private CustomerService customerService;

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
    void shouldCreateCustomerSuccessfully() {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Stark Industries",
            "  tony@STARK.com ",
            "+1-555-0100",
            "Malibu Point",
            "Preferred buyer"
        );

        when(customerRepository.existsByOrganizationIdAndEmail(orgId, "tony@stark.com")).thenReturn(false);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        Customer saved = new Customer(
            organization,
            "Stark Industries",
            "tony@stark.com",
            "+1-555-0100",
            "Malibu Point",
            "Preferred buyer"
        );
        saved.setId(UUID.randomUUID());

        when(customerRepository.save(any(Customer.class))).thenReturn(saved);

        CustomerResponse response = customerService.createCustomer(adminPrincipal, request);

        assertEquals("Stark Industries", response.name());
        assertEquals("tony@stark.com", response.email());
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void shouldRejectDuplicateEmailInOrganization() {
        CreateCustomerRequest request = new CreateCustomerRequest(
            "Duplicate Co",
            "dup@acme.com",
            null,
            null,
            null
        );

        when(customerRepository.existsByOrganizationIdAndEmail(orgId, "dup@acme.com")).thenReturn(true);

        assertThrows(BusinessException.class, () -> customerService.createCustomer(adminPrincipal, request));
        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void shouldSoftDeleteAndExcludeFromList() {
        UUID customerId = UUID.randomUUID();
        Customer customer = new Customer(organization, "Old Customer", "old@acme.com", null, null, null);
        customer.setId(customerId);
        customer.setActive(true);

        when(customerRepository.findByIdAndOrganizationIdAndActiveTrue(customerId, orgId))
            .thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerResponse deleted = customerService.deleteCustomer(adminPrincipal, customerId);
        assertFalse(deleted.active());

        Pageable pageable = PageRequest.of(0, 20);
        when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of()));

        Page<CustomerResponse> page = customerService.listCustomers(adminPrincipal, null, pageable);
        assertEquals(0, page.getTotalElements());
    }
}
