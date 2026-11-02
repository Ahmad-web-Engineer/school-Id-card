package com.platform.idcard.production.entity;

import com.platform.idcard.auth.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "production_updates")
public class ProductionUpdate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private ProductionBatch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "printed_quantity", nullable = false)
    private Integer printedQuantity = 0;

    @Column(name = "rejected_quantity", nullable = false)
    private Integer rejectedQuantity = 0;

    @Column(name = "pending_quantity", nullable = false)
    private Integer pendingQuantity = 0;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "evidence_file_id", length = 255)
    private String evidenceFileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public ProductionUpdate() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public ProductionBatch getBatch() {
        return batch;
    }

    public void setBatch(ProductionBatch batch) {
        this.batch = batch;
    }

    public User getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(User updatedBy) {
        this.updatedBy = updatedBy;
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

    public Integer getPendingQuantity() {
        return pendingQuantity;
    }

    public void setPendingQuantity(Integer pendingQuantity) {
        this.pendingQuantity = pendingQuantity;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getEvidenceFileId() {
        return evidenceFileId;
    }

    public void setEvidenceFileId(String evidenceFileId) {
        this.evidenceFileId = evidenceFileId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
