package com.platform.idcard.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.dto.RefreshTokenRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        if (!userRepository.existsByEmail("admin@platform.com")) {
            Role adminRole = roleRepository.findByRoleName("SUPER_ADMIN")
                    .orElseGet(() -> roleRepository.save(new Role(null, "SUPER_ADMIN", "Administrator")));

            User admin = new User();
            admin.setFullName("Super Admin");
            admin.setEmail("admin@platform.com");
            admin.setPasswordHash(passwordEncoder.encode("Password123!"));
            admin.setStatus("ACTIVE");
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
        }
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials returns 200 OK with tokens and user profile")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("admin@platform.com", "Password123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.data.user.email", is("admin@platform.com")))
                .andExpect(jsonPath("$.data.user.role", is("SUPER_ADMIN")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid password returns 401 UNAUTHORIZED envelope")
    void shouldRejectInvalidPassword() throws Exception {
        LoginRequest request = new LoginRequest("admin@platform.com", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with missing email fails Jakarta validation returning 400")
    void shouldFailValidationForBlankCredentials() throws Exception {
        LoginRequest request = new LoginRequest("", "");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Full Auth Flow: Login -> /auth/me with Bearer token -> Refresh -> Logout")
    void shouldCompleteFullAuthLifecycle() throws Exception {
        // 1. Login
        LoginRequest loginReq = new LoginRequest("admin@platform.com", "Password123!");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        String responseJson = loginResult.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseJson).path("data").path("accessToken").asText();
        String refreshToken = objectMapper.readTree(responseJson).path("data").path("refreshToken").asText();

        // 2. Access /auth/me with Bearer token
        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("admin@platform.com")))
                .andExpect(jsonPath("$.data.role", is("SUPER_ADMIN")));

        // 3. Refresh token
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(refreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andReturn();

        String newRefreshToken = objectMapper.readTree(refreshResult.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // 4. Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(newRefreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}
