package com.opsflow.inventory.domain;

import com.opsflow.common.domain.BaseEntity;
import com.opsflow.organizations.domain.Organization;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "inventory_movements")
public class InventoryMovement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_delta", nullable = false)
    private int quantityDelta;

    @Column(name = "previous_quantity", nullable = false)
    private int previousQuantity;

    @Column(name = "new_quantity", nullable = false)
    private int newQuantity;

    @Enumerated(EnumType.STRING)
    @Column(name = "movement_type", nullable = false, length = 50)
    private MovementType movementType;

    @Column(name = "reason")
    private String reason;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "performed_by_email", nullable = false)
    private String performedByEmail;

    public InventoryMovement() {
    }

    public InventoryMovement(
        Organization organization,
        Product product,
        int quantityDelta,
        int previousQuantity,
        int newQuantity,
        MovementType movementType,
        String reason,
        UUID performedBy,
        String performedByEmail
    ) {
        this.organization = organization;
        this.product = product;
        this.quantityDelta = quantityDelta;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.movementType = movementType;
        this.reason = reason;
        this.performedBy = performedBy;
        this.performedByEmail = performedByEmail;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantityDelta() {
        return quantityDelta;
    }

    public void setQuantityDelta(int quantityDelta) {
        this.quantityDelta = quantityDelta;
    }

    public int getPreviousQuantity() {
        return previousQuantity;
    }

    public void setPreviousQuantity(int previousQuantity) {
        this.previousQuantity = previousQuantity;
    }

    public int getNewQuantity() {
        return newQuantity;
    }

    public void setNewQuantity(int newQuantity) {
        this.newQuantity = newQuantity;
    }

    public MovementType getMovementType() {
        return movementType;
    }

    public void setMovementType(MovementType movementType) {
        this.movementType = movementType;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(UUID performedBy) {
        this.performedBy = performedBy;
    }

    public String getPerformedByEmail() {
        return performedByEmail;
    }

    public void setPerformedByEmail(String performedByEmail) {
        this.performedByEmail = performedByEmail;
    }
}
