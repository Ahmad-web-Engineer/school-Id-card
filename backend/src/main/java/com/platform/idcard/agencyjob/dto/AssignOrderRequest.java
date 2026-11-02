package com.platform.idcard.agencyjob.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AssignOrderRequest(
        @NotEmpty(message = "At least one agency assignment must be provided")
        @Valid
        List<AgencyAssignmentItemRequest> assignments
) {}
