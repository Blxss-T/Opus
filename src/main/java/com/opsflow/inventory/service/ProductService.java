package com.opsflow.inventory.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.common.jpa.FilterSpecs;
import com.opsflow.inventory.domain.InventoryMovement;
import com.opsflow.inventory.domain.MovementType;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.dto.AdjustStockRequest;
import com.opsflow.inventory.dto.CreateProductRequest;
import com.opsflow.inventory.dto.InventoryMovementResponse;
import com.opsflow.inventory.dto.ProductResponse;
import com.opsflow.inventory.dto.UpdateProductRequest;
import com.opsflow.inventory.repository.InventoryMovementRepository;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final OrganizationRepository organizationRepository;

    public ProductService(
        ProductRepository productRepository,
        InventoryMovementRepository inventoryMovementRepository,
        OrganizationRepository organizationRepository
    ) {
        this.productRepository = productRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.organizationRepository = organizationRepository;
    }

    @Transactional
    public ProductResponse createProduct(UserPrincipal principal, CreateProductRequest request) {
        UUID organizationId = principal.getOrganizationId();
        String normalizedSku = request.sku().trim().toUpperCase();

        if (productRepository.existsByOrganizationIdAndSkuIgnoreCase(organizationId, normalizedSku)) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                String.format("Product with SKU '%s' already exists in your organization", normalizedSku)
            );
        }

        Organization organization = organizationRepository.findById(organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Organization", "id", organizationId));

        Product product = new Product(
            organization,
            normalizedSku,
            request.name().trim(),
            request.description() != null ? request.description().trim() : null,
            request.category().trim(),
            request.unitPrice(),
            request.costPrice(),
            request.initialStock(),
            request.reorderLevel()
        );

        product = productRepository.save(product);

        if (request.initialStock() > 0) {
            InventoryMovement initialMovement = new InventoryMovement(
                organization,
                product,
                request.initialStock(),
                0,
                request.initialStock(),
                MovementType.INBOUND,
                "Initial inventory intake",
                principal.getId(),
                principal.getUsername()
            );
            inventoryMovementRepository.save(initialMovement);
        }

        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(UserPrincipal principal, UUID productId) {
        Product product = findProductForTenant(principal.getOrganizationId(), productId);
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> listProducts(UserPrincipal principal, String q, String category, Pageable pageable) {
        Specification<Product> spec = FilterSpecs.and(
            FilterSpecs.organizationIs(principal.getOrganizationId()),
            FilterSpecs.equalsValue("active", true),
            FilterSpecs.ilikeAny(q, "name", "sku"),
            FilterSpecs.equalsIgnoreCase("category", category)
        );
        Pageable effective = FilterSpecs.withDefaultSort(pageable, Sort.Order.asc("name"));
        return productRepository.findAll(spec, effective).map(ProductResponse::fromEntity);
    }

    @Transactional
    public ProductResponse updateProduct(UserPrincipal principal, UUID productId, UpdateProductRequest request) {
        Product product = findProductForTenant(principal.getOrganizationId(), productId);

        product.setName(request.name().trim());
        product.setDescription(request.description() != null ? request.description().trim() : null);
        product.setCategory(request.category().trim());
        product.setUnitPrice(request.unitPrice());
        product.setCostPrice(request.costPrice());
        product.setReorderLevel(request.reorderLevel());

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public ProductResponse deleteProduct(UserPrincipal principal, UUID productId) {
        Product product = findProductForTenant(principal.getOrganizationId(), productId);
        product.setActive(false);
        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public ProductResponse adjustStock(UserPrincipal principal, UUID productId, AdjustStockRequest request) {
        Product product = findProductForTenant(principal.getOrganizationId(), productId);

        int currentStock = product.getStockQuantity();
        int delta = request.quantityDelta();
        int newStock = currentStock + delta;

        if (newStock < 0) {
            throw new BusinessException(
                ErrorCode.BUSINESS_RULE_VIOLATION,
                String.format(
                    "Insufficient inventory: current stock is %d, requested adjustment delta is %d",
                    currentStock,
                    delta
                )
            );
        }

        product.setStockQuantity(newStock);
        Product savedProduct = productRepository.save(product);

        InventoryMovement movement = new InventoryMovement(
            product.getOrganization(),
            savedProduct,
            delta,
            currentStock,
            newStock,
            request.movementType(),
            request.reason().trim(),
            principal.getId(),
            principal.getUsername()
        );
        inventoryMovementRepository.save(movement);

        return ProductResponse.fromEntity(savedProduct);
    }

    @Transactional(readOnly = true)
    public Page<InventoryMovementResponse> getProductMovements(
        UserPrincipal principal,
        UUID productId,
        MovementType type,
        Pageable pageable
    ) {
        // Ensure product exists and belongs to the caller's organization
        findProductForTenant(principal.getOrganizationId(), productId);

        Specification<InventoryMovement> spec = FilterSpecs.and(
            FilterSpecs.organizationIs(principal.getOrganizationId()),
            FilterSpecs.equalsValue("movementType", type),
            (root, query, cb) -> cb.equal(root.get("product").get("id"), productId)
        );
        Pageable effective = FilterSpecs.withDefaultSort(pageable, Sort.Order.desc("createdAt"));
        return inventoryMovementRepository.findAll(spec, effective)
            .map(InventoryMovementResponse::fromEntity);
    }

    private Product findProductForTenant(UUID organizationId, UUID productId) {
        return productRepository.findByIdAndOrganizationIdAndActiveTrue(productId, organizationId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
    }
}
