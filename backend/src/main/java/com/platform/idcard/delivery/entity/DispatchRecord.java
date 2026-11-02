package com.platform.idcard.delivery.entity;

import com.platform.idcard.agencyjob.entity.AgencyAssignment;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.order.entity.PrintingOrder;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "dispatch_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_dispatch_number", columnNames = {"dispatch_number"})
        }
)
public class DispatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id")
    private AgencyAssignment assignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PrintingOrder order;

    @Column(name = "dispatch_number", nullable = false, length = 50)
    private String dispatchNumber;

    @Column(name = "courier_name", nullable = false, length = 100)
    private String courierName;

    @Column(name = "tracking_number", nullable = false, length = 100)
    private String trackingNumber;

    @Column(name = "dispatch_quantity", nullable = false)
    private Integer dispatchQuantity;

    @Column(name = "dispatch_date", nullable = false)
    private OffsetDateTime dispatchDate = OffsetDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_status", nullable = false, length = 50)
    private DispatchStatus dispatchStatus = DispatchStatus.IN_TRANSIT;

    @Enumerated(EnumType.STRING)
    @Column(name = "dispatch_type", nullable = false, length = 50)
    private DispatchType dispatchType = DispatchType.AGENCY_TO_SCHOOL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatched_by")
    private User dispatchedBy;

    @Column(name = "dispatch_notes", columnDefinition = "TEXT")
    private String dispatchNotes;

    @Column(name = "proof_file_id")
    private UUID proofFileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public DispatchRecord() {}

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

    public PrintingOrder getOrder() {
        return order;
    }

    public void setOrder(PrintingOrder order) {
        this.order = order;
    }

    public String getDispatchNumber() {
        return dispatchNumber;
    }

    public void setDispatchNumber(String dispatchNumber) {
        this.dispatchNumber = dispatchNumber;
    }

    public String getCourierName() {
        return courierName;
    }

    public void setCourierName(String courierName) {
        this.courierName = courierName;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Integer getDispatchQuantity() {
        return dispatchQuantity;
    }

    public void setDispatchQuantity(Integer dispatchQuantity) {
        this.dispatchQuantity = dispatchQuantity;
    }

    public OffsetDateTime getDispatchDate() {
        return dispatchDate;
    }

    public void setDispatchDate(OffsetDateTime dispatchDate) {
        this.dispatchDate = dispatchDate;
    }

    public DispatchStatus getDispatchStatus() {
        return dispatchStatus;
    }

    public void setDispatchStatus(DispatchStatus dispatchStatus) {
        this.dispatchStatus = dispatchStatus;
    }

    public DispatchType getDispatchType() {
        return dispatchType;
    }

    public void setDispatchType(DispatchType dispatchType) {
        this.dispatchType = dispatchType;
    }

    public User getDispatchedBy() {
        return dispatchedBy;
    }

    public void setDispatchedBy(User dispatchedBy) {
        this.dispatchedBy = dispatchedBy;
    }

    public String getDispatchNotes() {
        return dispatchNotes;
    }

    public void setDispatchNotes(String dispatchNotes) {
        this.dispatchNotes = dispatchNotes;
    }

    public UUID getProofFileId() {
        return proofFileId;
    }

    public void setProofFileId(UUID proofFileId) {
        this.proofFileId = proofFileId;
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
