package com.platform.idcard.organization.dto;

import com.platform.idcard.organization.entity.Agency;

import java.time.Instant;
import java.util.UUID;

public record AgencyResponse(
        UUID id,
        String agencyName,
        String contactPerson,
        String email,
        String mobile,
        String address,
        Integer productionCapacity,
        String capabilities,
        String status,
        Instant createdAt
) {
    public static AgencyResponse fromEntity(Agency agency) {
        return new AgencyResponse(
                agency.getId(),
                agency.getAgencyName(),
                agency.getContactPerson(),
                agency.getEmail(),
                agency.getMobile(),
                agency.getAddress(),
                agency.getProductionCapacity(),
                agency.getCapabilities(),
                agency.getStatus(),
                agency.getCreatedAt()
        );
    }
}
