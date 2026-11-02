package com.platform.idcard.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AdditionalItemRequest {

    @NotBlank(message = "Product type is required")
    private String productType;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;

    public AdditionalItemRequest() {}

    public AdditionalItemRequest(String productType, Integer quantity) {
        this.productType = productType;
        this.quantity = quantity;
    }

    public String productType() {
        return productType;
    }

    public Integer quantity() {
        return quantity;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public void setItemType(String itemType) {
        if (this.productType == null) {
            this.productType = itemType;
        }
    }

    public void setType(String type) {
        if (this.productType == null) {
            this.productType = type;
        }
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
