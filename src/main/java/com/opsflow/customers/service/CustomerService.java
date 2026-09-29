package com.opsflow.customers.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.customers.domain.Customer;
import com.opsflow.customers.dto.CreateCustomerRequest;
import com.opsflow.customers.dto.CustomerResponse;
import com.opsflow.customers.dto.UpdateCustomerRequest;
import com.opsflow.customers.repository.CustomerRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OrganizationRepository organizationRepository;

    public CustomerService(
        CustomerRepository customerRepository,
        OrganizationRepository organizationRepository
    ) {
        this.customerRepository = customerRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public CustomerResponse createCustomer(UserPrincipal principal, CreateCustomerRequest request) {
        UUID organizationId = principal.getOrganizationId();
        String normalizedEmail = normalizeEmail(request.email());

        if (customerRepository.existsByOrganizationIdAndEmail(organizationId, normalizedEmail)) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                String.format("Customer with email '%s' already exists in your organization", normalizedEmail)
            );
        }

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        Customer customer = new Customer(
            organization,
            request.name().trim(),
            normalizedEmail,
            trimToNull(request.phone()),
            trimToNull(request.address()),
            trimToNull(request.notes())
        );

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UserPrincipal principal, UUID customerId) {
        Customer customer = findCustomerForTenant(principal.getOrganizationId(), customerId);
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> listCustomers(UserPrincipal principal, Pageable pageable) {
        return customerRepository.findByOrganizationIdAndActiveTrue(principal.getOrganizationId(), pageable)
            .map(CustomerResponse::fromEntity);
    }

    @Transactional
    public CustomerResponse updateCustomer(UserPrincipal principal, UUID customerId, UpdateCustomerRequest request) {
        UUID organizationId = principal.getOrganizationId();
        Customer customer = findCustomerForTenant(organizationId, customerId);
        String normalizedEmail = normalizeEmail(request.email());

        if (customerRepository.existsByOrganizationIdAndEmailAndIdNot(organizationId, normalizedEmail, customerId)) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                String.format("Another customer with email '%s' already exists in your organization", normalizedEmail)
            );
        }

        customer.setName(request.name().trim());
        customer.setEmail(normalizedEmail);
        customer.setPhone(trimToNull(request.phone()));
        customer.setAddress(trimToNull(request.address()));
        customer.setNotes(trimToNull(request.notes()));

        Customer saved = customerRepository.save(customer);
        return CustomerResponse.fromEntity(saved);
    }

    @Transactional
    public CustomerResponse deleteCustomer(UserPrincipal principal, UUID customerId) {
        Customer customer = findCustomerForTenant(principal.getOrganizationId(), customerId);
        customer.setActive(false);
        Customer saved = customerRepository.save(customer);
        return CustomerResponse.fromEntity(saved);
    }

    private Customer findCustomerForTenant(UUID organizationId, UUID customerId) {
        return customerRepository.findByIdAndOrganizationIdAndActiveTrue(customerId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Customer", "id", customerId));
    }

    private static String normalizeEmail(String email) {
        return email.toLowerCase(Locale.ROOT).trim();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
