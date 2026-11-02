package com.platform.idcard.health;

import com.platform.idcard.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health", description = "System health check endpoints")
public class HealthController {

    @Value("${spring.application.name:school-id-card-platform}")
    private String applicationName;

    @GetMapping
    @Operation(summary = "Health check", description = "Returns health status, service name, version and current timestamp")
    public ResponseEntity<ApiResponse<HealthResponse>> checkHealth() {
        HealthResponse response = HealthResponse.up(applicationName, "1.0.0");
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
