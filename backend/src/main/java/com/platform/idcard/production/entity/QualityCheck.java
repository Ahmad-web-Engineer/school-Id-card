package com.platform.idcard.production.entity;

import com.platform.idcard.agencyjob.entity.AgencyAssignment;
import com.platform.idcard.auth.entity.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "quality_checks")
public class QualityCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private AgencyAssignment assignment;

    @Column(name = "check_number", nullable = false, unique = true, length = 50)
    private String checkNumber;

    @Column(name = "inspected_quantity", nullable = false)
    private Integer inspectedQuantity;

    @Column(name = "accepted_quantity", nullable = false)
    private Integer acceptedQuantity;

    @Column(name = "rejected_quantity", nullable = false)
    private Integer rejectedQuantity;

    @Column(name = "defect_description", columnDefinition = "TEXT")
    private String defectDescription;

    @Enumerated(EnumType.STRING)
    @Column(name = "defect_category", length = 50)
    private DefectCategory defectCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspected_by")
    private User inspectedBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "inspection_status", nullable = false, length = 50)
    private QualityCheckStatus inspectionStatus = QualityCheckStatus.PASSED;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "inspected_at", nullable = false)
    private OffsetDateTime inspectedAt = OffsetDateTime.now();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public QualityCheck() {
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

    public String getCheckNumber() {
        return checkNumber;
    }

    public void setCheckNumber(String checkNumber) {
        this.checkNumber = checkNumber;
    }

    public Integer getInspectedQuantity() {
        return inspectedQuantity;
    }

    public void setInspectedQuantity(Integer inspectedQuantity) {
        this.inspectedQuantity = inspectedQuantity;
    }

    public Integer getAcceptedQuantity() {
        return acceptedQuantity;
    }

    public void setAcceptedQuantity(Integer acceptedQuantity) {
        this.acceptedQuantity = acceptedQuantity;
    }

    public Integer getRejectedQuantity() {
        return rejectedQuantity;
    }

    public void setRejectedQuantity(Integer rejectedQuantity) {
        this.rejectedQuantity = rejectedQuantity;
    }

    public String getDefectDescription() {
        return defectDescription;
    }

    public void setDefectDescription(String defectDescription) {
        this.defectDescription = defectDescription;
    }

    public DefectCategory getDefectCategory() {
        return defectCategory;
    }

    public void setDefectCategory(DefectCategory defectCategory) {
        this.defectCategory = defectCategory;
    }

    public User getInspectedBy() {
        return inspectedBy;
    }

    public void setInspectedBy(User inspectedBy) {
        this.inspectedBy = inspectedBy;
    }

    public QualityCheckStatus getInspectionStatus() {
        return inspectionStatus;
    }

    public void setInspectionStatus(QualityCheckStatus inspectionStatus) {
        this.inspectionStatus = inspectionStatus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public OffsetDateTime getInspectedAt() {
        return inspectedAt;
    }

    public void setInspectedAt(OffsetDateTime inspectedAt) {
        this.inspectedAt = inspectedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
