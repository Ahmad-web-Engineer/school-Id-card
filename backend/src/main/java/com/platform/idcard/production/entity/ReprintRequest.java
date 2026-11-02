package com.platform.idcard.production.entity;

import com.platform.idcard.agencyjob.entity.AgencyAssignment;
import com.platform.idcard.auth.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "reprint_requests")
public class ReprintRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private AgencyAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "quality_check_id")
    private QualityCheck qualityCheck;

    @Column(name = "request_number", nullable = false, unique = true, length = 50)
    private String requestNumber;

    @Column(name = "replacement_quantity", nullable = false)
    private Integer replacementQuantity;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_bearer", nullable = false, length = 50)
    private CostBearer costBearer = CostBearer.AGENCY;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 50)
    private ReprintStatus approvalStatus = ReprintStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private OffsetDateTime approvedAt;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public ReprintRequest() {
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

    public QualityCheck getQualityCheck() {
        return qualityCheck;
    }

    public void setQualityCheck(QualityCheck qualityCheck) {
        this.qualityCheck = qualityCheck;
    }

    public String getRequestNumber() {
        return requestNumber;
    }

    public void setRequestNumber(String requestNumber) {
        this.requestNumber = requestNumber;
    }

    public Integer getReplacementQuantity() {
        return replacementQuantity;
    }

    public void setReplacementQuantity(Integer replacementQuantity) {
        this.replacementQuantity = replacementQuantity;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public CostBearer getCostBearer() {
        return costBearer;
    }

    public void setCostBearer(CostBearer costBearer) {
        this.costBearer = costBearer;
    }

    public ReprintStatus getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(ReprintStatus approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User approvedBy) {
        this.approvedBy = approvedBy;
    }

    public OffsetDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(OffsetDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
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
