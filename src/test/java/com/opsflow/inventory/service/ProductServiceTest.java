package com.opsflow.inventory.service;

import com.opsflow.auth.security.UserPrincipal;
import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ResourceNotFoundException;
import com.opsflow.inventory.domain.InventoryMovement;
import com.opsflow.inventory.domain.MovementType;
import com.opsflow.inventory.domain.Product;
import com.opsflow.inventory.dto.AdjustStockRequest;
import com.opsflow.inventory.dto.CreateProductRequest;
import com.opsflow.inventory.dto.ProductResponse;
import com.opsflow.inventory.dto.UpdateProductRequest;
import com.opsflow.inventory.repository.InventoryMovementRepository;
import com.opsflow.inventory.repository.ProductRepository;
import com.opsflow.organizations.domain.Organization;
import com.opsflow.organizations.repository.OrganizationRepository;
import com.opsflow.users.domain.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @InjectMocks
    private ProductService productService;

    private Organization organization;
    private UserPrincipal adminPrincipal;
    private UUID orgId;
    private UUID userId;

    @BeforeEach
    void setUp() {
        orgId = UUID.randomUUID();
        userId = UUID.randomUUID();

        organization = new Organization("Acme Corp", "acme-corp");
        organization.setId(orgId);

        adminPrincipal = new UserPrincipal(
            userId,
            orgId,
            "admin@acme.com",
            "hashedPass",
            Role.ORG_ADMIN,
            true
        );
    }

    @Test
    void shouldCreateProductAndRecordInitialMovementSuccessfully() {
        CreateProductRequest request = new CreateProductRequest(
            "WIDGET-001",
            "Industrial Widget",
            "Heavy duty widget",
            "Hardware",
            new BigDecimal("99.99"),
            new BigDecimal("45.00"),
            50,
            10
        );

        when(productRepository.existsByOrganizationIdAndSkuIgnoreCase(orgId, "WIDGET-001")).thenReturn(false);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        Product savedProduct = new Product(
            organization,
            "WIDGET-001",
            "Industrial Widget",
            "Heavy duty widget",
            "Hardware",
            new BigDecimal("99.99"),
            new BigDecimal("45.00"),
            50,
            10
        );
        savedProduct.setId(UUID.randomUUID());

        when(productRepository.save(any(Product.class))).thenReturn(savedProduct);

        ProductResponse response = productService.createProduct(adminPrincipal, request);

        assertNotNull(response);
        assertEquals("WIDGET-001", response.sku());
        assertEquals(50, response.stockQuantity());

        verify(productRepository).save(any(Product.class));
        verify(inventoryMovementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void shouldThrowExceptionWhenCreatingProductWithDuplicateSku() {
        CreateProductRequest request = new CreateProductRequest(
            "DUPLICATE-SKU",
            "Widget",
            "Desc",
            "Hardware",
            BigDecimal.TEN,
            BigDecimal.ONE,
            0,
            5
        );

        when(productRepository.existsByOrganizationIdAndSkuIgnoreCase(orgId, "DUPLICATE-SKU")).thenReturn(true);

        assertThrows(BusinessException.class, () -> productService.createProduct(adminPrincipal, request));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void shouldAdjustStockPositivelyAndRecordMovement() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(
            organization,
            "SKU-1",
            "Widget",
            "Desc",
            "Hardware",
            BigDecimal.TEN,
            BigDecimal.ONE,
            20,
            5
        );
        product.setId(productId);

        when(productRepository.findByIdAndOrganizationIdAndActiveTrue(productId, orgId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdjustStockRequest request = new AdjustStockRequest(15, MovementType.INBOUND, "Restocked shipment");

        ProductResponse response = productService.adjustStock(adminPrincipal, productId, request);

        assertEquals(35, response.stockQuantity());
        verify(inventoryMovementRepository).save(any(InventoryMovement.class));
    }

    @Test
    void shouldThrowExceptionWhenDeductingStockBelowZero() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(
            organization,
            "SKU-1",
            "Widget",
            "Desc",
            "Hardware",
            BigDecimal.TEN,
            BigDecimal.ONE,
            5,
            5
        );
        product.setId(productId);

        when(productRepository.findByIdAndOrganizationIdAndActiveTrue(productId, orgId)).thenReturn(Optional.of(product));

        AdjustStockRequest request = new AdjustStockRequest(-10, MovementType.OUTBOUND, "Customer order");

        assertThrows(BusinessException.class, () -> productService.adjustStock(adminPrincipal, productId, request));
        verify(inventoryMovementRepository, never()).save(any(InventoryMovement.class));
    }

    @Test
    void shouldSoftDeleteProduct() {
        UUID productId = UUID.randomUUID();
        Product product = new Product(
            organization,
            "SKU-1",
            "Widget",
            "Desc",
            "Hardware",
            BigDecimal.TEN,
            BigDecimal.ONE,
            10,
            5
        );
        product.setId(productId);
        product.setActive(true);

        when(productRepository.findByIdAndOrganizationIdAndActiveTrue(productId, orgId)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductResponse response = productService.deleteProduct(adminPrincipal, productId);

        assertFalse(response.active());
        verify(productRepository).save(product);
    }
}
