package com.platform.idcard.auth.controller;

import com.platform.idcard.auth.dto.*;
import com.platform.idcard.auth.service.AuthService;
import com.platform.idcard.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "User login, registration, token refresh, profile and session management")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticates user credentials and issues access & refresh tokens")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/register-school")
    @Operation(summary = "Public School Self-Registration", description = "Allows a new school to register with admin credentials")
    public ResponseEntity<ApiResponse<LoginResponse>> registerSchool(@Valid @RequestBody RegisterSchoolRequest request) {
        LoginResponse response = authService.registerSchool(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change own password", description = "Allows logged-in user to update their password")
    public ResponseEntity<ApiResponse<Map<String, String>>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Password changed successfully")));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Rotates refresh token and issues a new access token")
    public ResponseEntity<ApiResponse<TokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    @Operation(summary = "User logout", description = "Revokes user refresh token")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        String tokenStr = request != null ? request.refreshToken() : null;
        authService.logout(tokenStr);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Logged out successfully")));
    }

    @GetMapping("/me")
    @Operation(summary = "Current user profile", description = "Returns profile and role data of currently authenticated user")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> getMe() {
        UserSummaryResponse profile = authService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }
}
