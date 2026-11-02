package com.platform.idcard.template.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "template_versions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_template_version", columnNames = {"template_id", "version_number"})
        }
)
public class TemplateVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private CardTemplate template;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(name = "card_width_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal cardWidthMm = new BigDecimal("85.60");

    @Column(name = "card_height_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal cardHeightMm = new BigDecimal("54.00");

    @Column(name = "bleed_margin_mm", nullable = false, precision = 8, scale = 2)
    private BigDecimal bleedMarginMm = new BigDecimal("3.00");

    @Column(name = "front_design_json", nullable = false, columnDefinition = "TEXT")
    private String frontDesignJson;

    @Column(name = "back_design_json", nullable = false, columnDefinition = "TEXT")
    private String backDesignJson;

    @Column(name = "approval_status", nullable = false, length = 50)
    private String approvalStatus = TemplateApprovalStatus.DRAFT.name();

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public TemplateVersion() {}

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (approvalStatus == null) approvalStatus = TemplateApprovalStatus.DRAFT.name();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CardTemplate getTemplate() {
        return template;
    }

    public void setTemplate(CardTemplate template) {
        this.template = template;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public BigDecimal getCardWidthMm() {
        return cardWidthMm;
    }

    public void setCardWidthMm(BigDecimal cardWidthMm) {
        this.cardWidthMm = cardWidthMm;
    }

    public BigDecimal getCardHeightMm() {
        return cardHeightMm;
    }

    public void setCardHeightMm(BigDecimal cardHeightMm) {
        this.cardHeightMm = cardHeightMm;
    }

    public BigDecimal getBleedMarginMm() {
        return bleedMarginMm;
    }

    public void setBleedMarginMm(BigDecimal bleedMarginMm) {
        this.bleedMarginMm = bleedMarginMm;
    }

    public String getFrontDesignJson() {
        return frontDesignJson;
    }

    public void setFrontDesignJson(String frontDesignJson) {
        this.frontDesignJson = frontDesignJson;
    }

    public String getBackDesignJson() {
        return backDesignJson;
    }

    public void setBackDesignJson(String backDesignJson) {
        this.backDesignJson = backDesignJson;
    }

    public String getApprovalStatus() {
        return approvalStatus;
    }

    public void setApprovalStatus(String approvalStatus) {
        this.approvalStatus = approvalStatus;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
