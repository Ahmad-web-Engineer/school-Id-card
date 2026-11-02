package com.platform.idcard.agencyjob.entity;

import com.platform.idcard.order.entity.PrintingOrderItem;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agency_job_items")
public class AgencyJobItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private AgencyAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false)
    private PrintingOrderItem orderItem;

    @Column(name = "assigned_quantity", nullable = false)
    private Integer assignedQuantity = 0;

    @Column(name = "printed_quantity", nullable = false)
    private Integer printedQuantity = 0;

    @Column(name = "rejected_quantity", nullable = false)
    private Integer rejectedQuantity = 0;

    @Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity = 0;

    @Column(name = "pending_quantity", nullable = false)
    private Integer pendingQuantity = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public AgencyJobItem() {
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public AgencyAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(AgencyAssignment assignment) {
        this.assignment = assignment;
    }

    public PrintingOrderItem getOrderItem() {
        return orderItem;
    }

    public void setOrderItem(PrintingOrderItem orderItem) {
        this.orderItem = orderItem;
    }

    public Integer getAssignedQuantity() {
        return assignedQuantity;
    }

    public void setAssignedQuantity(Integer assignedQuantity) {
        this.assignedQuantity = assignedQuantity;
    }

    public Integer getPrintedQuantity() {
        return printedQuantity;
    }

    public void setPrintedQuantity(Integer printedQuantity) {
        this.printedQuantity = printedQuantity;
    }

    public Integer getRejectedQuantity() {
        return rejectedQuantity;
    }

    public void setRejectedQuantity(Integer rejectedQuantity) {
        this.rejectedQuantity = rejectedQuantity;
    }

    public Integer getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public void setAcceptedQuantity(Integer acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }

    public Integer getPendingQuantity() {
        return pendingQuantity;
    }

    public void setPendingQuantity(Integer pendingQuantity) {
        this.pendingQuantity = pendingQuantity;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
