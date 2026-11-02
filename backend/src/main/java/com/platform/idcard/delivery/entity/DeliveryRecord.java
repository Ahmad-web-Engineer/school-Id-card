package com.platform.idcard.delivery.entity;

import com.platform.idcard.auth.entity.User;
import com.platform.idcard.order.entity.PrintingOrder;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "delivery_records",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_delivery_number", columnNames = {"delivery_number"})
        }
)
public class DeliveryRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private PrintingOrder order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dispatch_id")
    private DispatchRecord dispatch;

    @Column(name = "delivery_number", nullable = false, length = 50)
    private String deliveryNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_model", nullable = false, length = 50)
    private DeliveryModel deliveryModel = DeliveryModel.DIRECT_TO_SCHOOL;

    @Column(name = "delivery_quantity", nullable = false)
    private Integer deliveryQuantity;

    @Column(name = "received_by", nullable = false, length = 100)
    private String receivedBy;

    @Column(name = "received_mobile", length = 20)
    private String receivedMobile;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 50)
    private DeliveryStatus deliveryStatus = DeliveryStatus.CONFIRMED;

    @Column(name = "delivered_at", nullable = false)
    private OffsetDateTime deliveredAt = OffsetDateTime.now();

    @Column(name = "delivery_notes", columnDefinition = "TEXT")
    private String deliveryNotes;

    @Column(name = "proof_of_delivery_file_id")
    private UUID proofOfDeliveryFileId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private User closedBy;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Column(name = "closure_notes", columnDefinition = "TEXT")
    private String closureNotes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    public DeliveryRecord() {}

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

    public PrintingOrder getOrder() {
        return order;
    }

    public void setOrder(PrintingOrder order) {
        this.order = order;
    }

    public DispatchRecord getDispatch() {
        return dispatch;
    }

    public void setDispatch(DispatchRecord dispatch) {
        this.dispatch = dispatch;
    }

    public String getDeliveryNumber() {
        return deliveryNumber;
    }

    public void setDeliveryNumber(String deliveryNumber) {
        this.deliveryNumber = deliveryNumber;
    }

    public DeliveryModel getDeliveryModel() {
        return deliveryModel;
    }

    public void setDeliveryModel(DeliveryModel deliveryModel) {
        this.deliveryModel = deliveryModel;
    }

    public Integer getDeliveryQuantity() {
        return deliveryQuantity;
    }

    public void setDeliveryQuantity(Integer deliveryQuantity) {
        this.deliveryQuantity = deliveryQuantity;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public void setReceivedBy(String receivedBy) {
        this.receivedBy = receivedBy;
    }

    public String getReceivedMobile() {
        return receivedMobile;
    }

    public void setReceivedMobile(String receivedMobile) {
        this.receivedMobile = receivedMobile;
    }

    public DeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(DeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public OffsetDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(OffsetDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }

    public UUID getProofOfDeliveryFileId() {
        return proofOfDeliveryFileId;
    }

    public void setProofOfDeliveryFileId(UUID proofOfDeliveryFileId) {
        this.proofOfDeliveryFileId = proofOfDeliveryFileId;
    }

    public User getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(User closedBy) {
        this.closedBy = closedBy;
    }

    public OffsetDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(OffsetDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosureNotes() {
        return closureNotes;
    }

    public void setClosureNotes(String closureNotes) {
        this.closureNotes = closureNotes;
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
