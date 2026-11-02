package com.platform.idcard.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GlobalExceptionHandlerTest.TestEndpointConfig.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class TestEndpointConfig {
        @RestController
        @RequestMapping("/api/v1/test-exceptions")
        static class TestExceptionController {
            @GetMapping("/not-found")
            public void notFound() {
                throw new ResourceNotFoundException("Student record not found.");
            }

            @GetMapping("/conflict")
            public void conflict() {
                throw new BusinessConflictException(ErrorCode.RESOURCE_CONFLICT, "Admission number already exists.");
            }

            @GetMapping("/validation")
            public void validation() {
                throw new ValidationException("Field 'email' is invalid.");
            }

            @GetMapping("/admin-only")
            @PreAuthorize("hasRole('SUPER_ADMIN')")
            public String adminOnly() {
                return "admin-content";
            }
        }
    }

    @Test
    @DisplayName("Accessing protected path without credentials returns 401 with standard error envelope")
    void shouldReturn401UnauthorizedEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @WithMockUser(username = "school_user", roles = {"SCHOOL"})
    @DisplayName("Accessing endpoint with insufficient role returns 403 with standard error envelope")
    void shouldReturn403ForbiddenWhenRoleInsufficient() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/admin-only")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")))
                .andExpect(jsonPath("$.message", is("You do not have permission to access this resource.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @WithMockUser(username = "test_user")
    @DisplayName("ResourceNotFoundException returns 404 with standard error envelope")
    void shouldReturn404ForResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/not-found")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Student record not found.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @WithMockUser(username = "test_user")
    @DisplayName("BusinessConflictException returns 409 with standard error envelope")
    void shouldReturn409ForConflictException() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/conflict")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_CONFLICT")))
                .andExpect(jsonPath("$.message", is("Admission number already exists.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @WithMockUser(username = "test_user")
    @DisplayName("ValidationException returns 400 with standard error envelope")
    void shouldReturn400ForValidationException() throws Exception {
        mockMvc.perform(get("/api/v1/test-exceptions/validation")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.message", is("Field 'email' is invalid.")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }
}
