package com.platform.idcard.agencyjob.entity;

import com.platform.idcard.auth.entity.User;
import com.platform.idcard.order.entity.PrintingOrder;
import com.platform.idcard.organization.entity.Agency;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "agency_assignments")
public class AgencyAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "assignment_number", nullable = false, unique = true, length = 50)
    private String assignmentNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PrintingOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agency_id", nullable = false)
    private Agency agency;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by")
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false)
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;

    @Column(name = "agreed_rate", nullable = false, precision = 14, scale = 2)
    private BigDecimal agreedRate = BigDecimal.ZERO;

    @Column(name = "total_agreed_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAgreedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false, length = 50)
    private AssignmentStatus assignmentStatus = AssignmentStatus.ASSIGNED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reassigned_from_id")
    private AgencyAssignment reassignedFrom;

    @Column(name = "reassignment_reason", columnDefinition = "TEXT")
    private String reassignmentReason;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AgencyJobItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public AgencyAssignment() {
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

    public String getAssignmentNumber() {
        return assignmentNumber;
    }

    public void setAssignmentNumber(String assignmentNumber) {
        this.assignmentNumber = assignmentNumber;
    }

    public PrintingOrder getOrder() {
        return order;
    }

    public void setOrder(PrintingOrder order) {
        this.order = order;
    }

    public Agency getAgency() {
        return agency;
    }

    public void setAgency(Agency agency) {
        this.agency = agency;
    }

    public User getAssignedBy() {
        return assignedBy;
    }

    public void setAssignedBy(User assignedBy) {
        this.assignedBy = assignedBy;
    }

    public OffsetDateTime getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(OffsetDateTime assignedAt) {
        this.assignedAt = assignedAt;
    }

    public LocalDate getExpectedDeliveryDate() {
        return expectedDeliveryDate;
    }

    public void setExpectedDeliveryDate(LocalDate expectedDeliveryDate) {
        this.expectedDeliveryDate = expectedDeliveryDate;
    }

    public BigDecimal getAgreedRate() {
        return agreedRate;
    }

    public void setAgreedRate(BigDecimal agreedRate) {
        this.agreedRate = agreedRate;
    }

    public BigDecimal getTotalAgreedAmount() {
        return totalAgreedAmount;
    }

    public void setTotalAgreedAmount(BigDecimal totalAgreedAmount) {
        this.totalAgreedAmount = totalAgreedAmount;
    }

    public AssignmentStatus getAssignmentStatus() {
        return assignmentStatus;
    }

    public void setAssignmentStatus(AssignmentStatus assignmentStatus) {
        this.assignmentStatus = assignmentStatus;
    }

    public AgencyAssignment getReassignedFrom() {
        return reassignedFrom;
    }

    public void setReassignedFrom(AgencyAssignment reassignedFrom) {
        this.reassignedFrom = reassignedFrom;
    }

    public String getReassignmentReason() {
        return reassignmentReason;
    }

    public void setReassignmentReason(String reassignmentReason) {
        this.reassignmentReason = reassignmentReason;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<AgencyJobItem> getItems() {
        return items;
    }

    public void setItems(List<AgencyJobItem> items) {
        this.items = items;
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
