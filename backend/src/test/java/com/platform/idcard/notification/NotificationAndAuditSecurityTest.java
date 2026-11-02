package com.platform.idcard.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.audit.service.AuditService;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.notification.dto.NotificationResponse;
import com.platform.idcard.notification.service.NotificationService;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
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

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationAndAuditSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID adminUserId;
    private String school1Token;
    private UUID school1UserId;
    private String school2Token;
    private UUID school2UserId;
    private String agencyToken;
    private UUID agencyUserId;

    @BeforeEach
    void setUp() throws Exception {
        Role adminRole = roleRepository.findByRoleName("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "SUPER_ADMIN", "Admin")));
        Role schoolRole = roleRepository.findByRoleName("SCHOOL")
                .orElseGet(() -> roleRepository.save(new Role(null, "SCHOOL", "School")));
        Role agencyRole = roleRepository.findByRoleName("PRINTING_AGENCY")
                .orElseGet(() -> roleRepository.save(new Role(null, "PRINTING_AGENCY", "Agency")));

        User adminUser = userRepository.findByEmail("admin@platform.com").orElse(null);
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setFullName("Super Admin");
            adminUser.setEmail("admin@platform.com");
            adminUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            adminUser.setStatus("ACTIVE");
            adminUser.setRoles(Set.of(adminRole));
            adminUser = userRepository.save(adminUser);
        }
        adminUserId = adminUser.getId();

        MvcResult adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@platform.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var s1Dto = schoolService.createSchool(new SchoolRequest(
                "School Notif 1 " + suffix, "SCH-N1-" + suffix, "10 School Way",
                "Metropolis", "State", "500001", "notif_sch1_" + suffix + "@test.edu",
                "9876543110", "ACTIVE"
        ));

        var s1User = schoolService.createSchoolUser(s1Dto.id(), new CreateOrganizationUserRequest(
                "School 1 Lead", "notif_usr1_" + suffix + "@test.edu", "9876543110", "Password123!", "Admin"
        ));
        school1UserId = s1User.id();

        MvcResult s1Login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("notif_usr1_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        school1Token = objectMapper.readTree(s1Login.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var s2Dto = schoolService.createSchool(new SchoolRequest(
                "School Notif 2 " + suffix, "SCH-N2-" + suffix, "20 School Way",
                "Metropolis", "State", "500001", "notif_sch2_" + suffix + "@test.edu",
                "9876543120", "ACTIVE"
        ));

        var s2User = schoolService.createSchoolUser(s2Dto.id(), new CreateOrganizationUserRequest(
                "School 2 Lead", "notif_usr2_" + suffix + "@test.edu", "9876543120", "Password123!", "Admin"
        ));
        school2UserId = s2User.id();

        MvcResult s2Login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("notif_usr2_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        school2Token = objectMapper.readTree(s2Login.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var agDto = agencyService.createAgency(new AgencyRequest(
                "Agency Notif " + suffix, "Contact", "notif_ag_" + suffix + "@print.com",
                "9876543130", "Industrial Area", 50000, "500002", "ACTIVE"
        ));

        var agUser = agencyService.createAgencyUser(agDto.id(), new CreateOrganizationUserRequest(
                "Agency Officer", "notif_agu_" + suffix + "@print.com", "9876543130", "Password123!", "Manager"
        ));
        agencyUserId = agUser.id();

        MvcResult agLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("notif_agu_" + suffix + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(agLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Rule 1: School user forbidden from Admin Audit Logs (403)")
    void schoolUserForbiddenFromAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Rule 1: Agency user forbidden from Admin Audit Logs (403)")
    void agencyUserForbiddenFromAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("Rule 1: School user forbidden from Admin Operational & Financial Reports (403)")
    void schoolUserForbiddenFromAdminReports() throws Exception {
        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/reports/financial")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rule 1: Agency user forbidden from School Reporting Summary (403)")
    void agencyUserForbiddenFromSchoolReports() throws Exception {
        mockMvc.perform(get("/api/v1/school/reports/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rule 1: School user forbidden from Agency Performance Report (403)")
    void schoolUserForbiddenFromAgencyReports() throws Exception {
        mockMvc.perform(get("/api/v1/agency/reports/performance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rule 2: User A cannot mark User B's notification as read (404/403)")
    void crossUserNotificationMarkReadForbidden() throws Exception {
        // Send a notification specifically to school1 user
        NotificationResponse n1 = notificationService.sendNotification(
                school1UserId,
                "ORDER_STATUS_CHANGED",
                "Order Update",
                "Your order status is now IN_PRODUCTION",
                "ORDER",
                UUID.randomUUID()
        );

        // School2 attempts to mark School1's notification as read
        mockMvc.perform(post("/api/v1/notifications/" + n1.id() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school2Token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("Rule 2: Notifications list is strictly isolated to current logged-in user")
    void notificationsStrictlyIsolatedToRecipient() throws Exception {
        // Send notification to school1
        notificationService.sendNotification(
                school1UserId, "ALERT_S1", "School 1 Alert", "Hello School 1", null, null
        );

        // Send notification to school2
        notificationService.sendNotification(
                school2UserId, "ALERT_S2", "School 2 Alert", "Hello School 2", null, null
        );

        // School1 should see only ALERT_S1
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].notificationType").value("ALERT_S1"));

        // School2 should see only ALERT_S2
        mockMvc.perform(get("/api/v1/notifications")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].notificationType").value("ALERT_S2"));
    }

    @Test
    @DisplayName("Unauthenticated requests return 401 UNAUTHORIZED")
    void unauthenticatedRequestsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/reports/operational"))
                .andExpect(status().isUnauthorized());
    }
}
