package com.platform.idcard.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "printing_order_items")
public class PrintingOrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PrintingOrder order;

    @Column(name = "product_type", nullable = false, length = 100)
    private String productType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit_selling_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitSellingPrice = BigDecimal.ZERO;

    @Column(name = "item_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal itemTotal = BigDecimal.ZERO;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PrintingOrderItem() {}

    public PrintingOrderItem(PrintingOrder order, String productType, Integer quantity, BigDecimal unitSellingPrice, BigDecimal itemTotal) {
        this.order = order;
        this.productType = productType;
        this.quantity = quantity;
        this.unitSellingPrice = unitSellingPrice != null ? unitSellingPrice : BigDecimal.ZERO;
        this.itemTotal = itemTotal != null ? itemTotal : BigDecimal.ZERO;
        this.createdAt = Instant.now();
    }

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (unitSellingPrice == null) unitSellingPrice = BigDecimal.ZERO;
        if (itemTotal == null) itemTotal = BigDecimal.ZERO;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PrintingOrder getOrder() {
        return order;
    }

    public void setOrder(PrintingOrder order) {
        this.order = order;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitSellingPrice() {
        return unitSellingPrice;
    }

    public void setUnitSellingPrice(BigDecimal unitSellingPrice) {
        this.unitSellingPrice = unitSellingPrice;
    }

    public BigDecimal getItemTotal() {
        return itemTotal;
    }

    public void setItemTotal(BigDecimal itemTotal) {
        this.itemTotal = itemTotal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
