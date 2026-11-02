package com.platform.idcard.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.*;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.template.dto.CreateCardTemplateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AccountManagementIntegrationTest {

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

    @Autowired
    private SchoolService schoolService;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        Role adminRole = roleRepository.findByRoleName("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "SUPER_ADMIN", "Platform Administrator")));
        if (roleRepository.findByRoleName("SCHOOL").isEmpty()) {
            roleRepository.save(new Role(null, "SCHOOL", "School Administrator"));
        }
        if (roleRepository.findByRoleName("PRINTING_AGENCY").isEmpty()) {
            roleRepository.save(new Role(null, "PRINTING_AGENCY", "Printing Agency Partner"));
        }

        User adminUser = userRepository.findByEmail("admin@platform.com").orElse(null);
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setFullName("Super Admin");
            adminUser.setEmail("admin@platform.com");
            adminUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            adminUser.setStatus("ACTIVE");
            adminUser.setRoles(Set.of(adminRole));
            userRepository.save(adminUser);
        } else {
            adminUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            userRepository.save(adminUser);
        }

        MvcResult adminRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@platform.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Public School Self-Registration: School registers publicly, gets tokens, and can use school endpoints")
    void testPublicSchoolSelfRegistration() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RegisterSchoolRequest request = new RegisterSchoolRequest(
                "St. Xavier Academy " + suffix,
                "SXA-" + suffix,
                "22 Cathedral Road",
                "Mumbai",
                "Maharashtra",
                "400001",
                "office_" + suffix + "@xavier.edu",
                "9876543210",
                "Sister Mary " + suffix,
                "admin_" + suffix + "@xavier.edu",
                "SecretPass123!",
                "9876543211",
                "Headmistress"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register-school")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.role", is("SCHOOL")))
                .andReturn();

        String token = objectMapper.readTree(result.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Verify the newly registered school user can immediately access their School Dashboard
        mockMvc.perform(get("/api/v1/school/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.enrolledStudents", is(0)));
    }

    @Test
    @DisplayName("Change Password Flow: User updates password, old password rejected, new password succeeds")
    void testChangePasswordFlow() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String email = "pwd_test_" + suffix + "@school.edu";

        // Register new school
        RegisterSchoolRequest regReq = new RegisterSchoolRequest(
                "Password Test School " + suffix,
                "PTS-" + suffix,
                "Street 1", "Pune", "Maharashtra", "411001",
                "info_" + suffix + "@school.edu", "9876500000",
                "Admin " + suffix, email, "InitialPass123!", "9876500000", "Principal"
        );
        MvcResult regRes = mockMvc.perform(post("/api/v1/auth/register-school")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated())
                .andReturn();
        String token = objectMapper.readTree(regRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Change password
        mockMvc.perform(post("/api/v1/auth/change-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest("InitialPass123!", "UpdatedPass999!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // Login with old password -> should fail
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "InitialPass123!"))))
                .andExpect(status().isUnauthorized());

        // Login with new password -> should succeed
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "UpdatedPass999!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("Admin User Management: Admin lists school users and resets user password")
    void testAdminUserListAndResetPassword() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var school = schoolService.createSchool(new SchoolRequest(
                "Reset Test School " + suffix, "RTS-" + suffix, "123 Test St",
                "City", "State", "560001", "reset_sch_" + suffix + "@school.edu",
                "9876500000", "ACTIVE"
        ));
        String userEmail = "reset_user_" + suffix + "@school.edu";
        schoolService.createSchoolUser(school.id(), new CreateOrganizationUserRequest(
                "Reset User", userEmail, "9876500000", "InitialPass123!", "Admin"
        ));

        // 1. List users for Greenwood
        MvcResult listRes = mockMvc.perform(get("/api/v1/admin/schools/" + school.id() + "/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andReturn();

        JsonNode users = objectMapper.readTree(listRes.getResponse().getContentAsString()).path("data");
        String userId = users.get(0).path("id").asText();

        // 2. Admin resets password for this user
        mockMvc.perform(post("/api/v1/admin/users/" + userId + "/reset-password")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ResetPasswordRequest("AdminResetPass99!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));

        // 3. User logs in with newly reset password
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(userEmail, "AdminResetPass99!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("Template Auto-Approve: When admin designs template with autoApprove=true, it is APPROVED immediately")
    void testTemplateAutoApprove() throws Exception {
        String templateName = "Auto Approved Deluxe " + UUID.randomUUID().toString().substring(0, 8);
        CreateCardTemplateRequest request = new CreateCardTemplateRequest(
                templateName,
                "STUDENT",
                null,
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                "{\"backgroundColor\":\"#ffffff\"}",
                "{\"backgroundColor\":\"#f0f0f0\"}",
                true // autoApprove = true
        );

        mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.versions[0].approvalStatus", is("APPROVED")))
                .andExpect(jsonPath("$.data.versions[0].approvedBy", notNullValue()))
                .andExpect(jsonPath("$.data.versions[0].approvedAt", notNullValue()));
    }
}
