package com.platform.idcard.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CreateOrderRequest {

    @NotNull(message = "Template version ID is required")
    private UUID templateVersionId;

    private String academicSession;
    private String deliveryModel;
    private String deliveryAddress;
    private String contactPerson;
    private String contactMobile;
    private String remarks;

    @NotEmpty(message = "At least one student must be selected for the order")
    private List<UUID> studentIds;

    @Valid
    private List<AdditionalItemRequest> additionalItems;

    public CreateOrderRequest() {}

    public CreateOrderRequest(
            UUID templateVersionId,
            String academicSession,
            String deliveryModel,
            String deliveryAddress,
            String contactPerson,
            String contactMobile,
            String remarks,
            List<UUID> studentIds,
            List<AdditionalItemRequest> additionalItems
    ) {
        this.templateVersionId = templateVersionId;
        this.academicSession = academicSession;
        this.deliveryModel = deliveryModel;
        this.deliveryAddress = deliveryAddress;
        this.contactPerson = contactPerson;
        this.contactMobile = contactMobile;
        this.remarks = remarks;
        this.studentIds = studentIds;
        this.additionalItems = additionalItems;
    }

    // Record-style accessor methods for backwards compatibility
    public UUID templateVersionId() { return templateVersionId; }
    public String academicSession() { return academicSession; }
    public String deliveryModel() { return deliveryModel; }
    public String deliveryAddress() { return deliveryAddress; }
    public String contactPerson() { return contactPerson; }
    public String contactMobile() { return contactMobile; }
    public String remarks() { return remarks; }
    public List<UUID> studentIds() { return studentIds; }
    public List<AdditionalItemRequest> additionalItems() { return additionalItems; }

    // Standard JavaBeans getters & setters
    public UUID getTemplateVersionId() { return templateVersionId; }
    public void setTemplateVersionId(UUID templateVersionId) { this.templateVersionId = templateVersionId; }

    public String getAcademicSession() { return academicSession; }
    public void setAcademicSession(String academicSession) { this.academicSession = academicSession; }

    public String getDeliveryModel() { return deliveryModel; }
    public void setDeliveryModel(String deliveryModel) { this.deliveryModel = deliveryModel; }

    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }

    public String getContactPerson() { return contactPerson; }
    public void setContactPerson(String contactPerson) { this.contactPerson = contactPerson; }

    public String getContactMobile() { return contactMobile; }
    public void setContactMobile(String contactMobile) { this.contactMobile = contactMobile; }
    public void setContactPhone(String contactPhone) {
        if (this.contactMobile == null) this.contactMobile = contactPhone;
    }
    public void setPhone(String phone) {
        if (this.contactMobile == null) this.contactMobile = phone;
    }

    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setNotes(String notes) {
        if (this.remarks == null) this.remarks = notes;
    }

    public List<UUID> getStudentIds() { return studentIds; }
    public void setStudentIds(List<UUID> studentIds) { this.studentIds = studentIds; }

    public List<AdditionalItemRequest> getAdditionalItems() { return additionalItems; }
    public void setAdditionalItems(List<AdditionalItemRequest> additionalItems) { this.additionalItems = additionalItems; }
}
