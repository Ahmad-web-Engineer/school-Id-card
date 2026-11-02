package com.platform.idcard.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.audit.service.AuditService;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationAndReportingIntegrationTest {

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
    private String schoolToken;
    private UUID schoolId;
    private UUID schoolUserId;
    private String agencyToken;
    private UUID agencyId;
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
        var sDto = schoolService.createSchool(new SchoolRequest(
                "Reporting Test School " + suffix, "SCH-RPT-" + suffix, "789 Campus Rd",
                "Metropolis", "State", "500001", "rpt_sch_" + suffix + "@test.edu",
                "9876543140", "ACTIVE"
        ));
        schoolId = sDto.id();

        var sUser = schoolService.createSchoolUser(schoolId, new CreateOrganizationUserRequest(
                "School Reporter", "rpt_usr_" + suffix + "@test.edu", "9876543140", "Password123!", "Admin"
        ));
        schoolUserId = sUser.id();

        MvcResult sLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("rpt_usr_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolToken = objectMapper.readTree(sLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var agDto = agencyService.createAgency(new AgencyRequest(
                "Reporting Agency " + suffix, "Contact", "rpt_ag_" + suffix + "@print.com",
                "9876543150", "Industrial Area", 50000, "500002", "ACTIVE"
        ));
        agencyId = agDto.id();

        var agUser = agencyService.createAgencyUser(agencyId, new CreateOrganizationUserRequest(
                "Agency Reporter", "rpt_agu_" + suffix + "@print.com", "9876543150", "Password123!", "Manager"
        ));
        agencyUserId = agUser.id();

        MvcResult agLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("rpt_agu_" + suffix + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(agLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("End-to-End Notifications, Audit Trail, and Cross-Role Reporting")
    void testNotificationsAuditAndReportingLifecycle() throws Exception {
        // 1. Record an audit log event
        auditService.recordAudit(
                adminUserId,
                "Super Admin",
                "SUPER_ADMIN",
                "SECURITY_POLICY_UPDATE",
                "POLICY",
                "POL-001",
                "{\"status\": \"ENFORCED\"}",
                "127.0.0.1"
        );

        // 2. Admin retrieves audit log and verifies record
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .param("action", "SECURITY_POLICY_UPDATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].action").value("SECURITY_POLICY_UPDATE"))
                .andExpect(jsonPath("$.data.data[0].entityType").value("POLICY"))
                .andExpect(jsonPath("$.data.data[0].entityId").value("POL-001"));

        // 3. Send 2 notifications to School User
        var notif1 = notificationService.sendNotification(
                schoolUserId,
                "ORDER_CONFIRMED",
                "Order Verified",
                "Your order has passed student data review",
                "ORDER",
                UUID.randomUUID()
        );

        var notif2 = notificationService.sendNotification(
                schoolUserId,
                "INVOICE_GENERATED",
                "Invoice Issued",
                "New tax invoice is available for download",
                "INVOICE",
                UUID.randomUUID()
        );

        // 4. Verify unread count is 2
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unreadCount").value(2));

        // 5. School marks first notification as read
        mockMvc.perform(post("/api/v1/notifications/" + notif1.id() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.read").value(true));

        // 6. Verify unread count is now 1
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(1));

        // 7. School marks all as read
        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.markedReadCount").value(1));

        // 8. Verify unread count is now 0
        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(0));

        // 9. Admin Operational Report
        mockMvc.perform(get("/api/v1/admin/reports/operational")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.activeSchools").isNumber())
                .andExpect(jsonPath("$.data.activeAgencies").isNumber())
                .andExpect(jsonPath("$.data.averageQualityPassRate").isNumber());

        // 10. Admin Financial Report
        mockMvc.perform(get("/api/v1/admin/reports/financial")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.grossRevenue").isNumber())
                .andExpect(jsonPath("$.data.agencyDirectCosts").isNumber())
                .andExpect(jsonPath("$.data.grossContribution").isNumber());

        // 11. School Reporting Summary
        mockMvc.perform(get("/api/v1/school/reports/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalStudents").isNumber())
                .andExpect(jsonPath("$.data.totalOrders").isNumber())
                .andExpect(jsonPath("$.data.totalBilled").isNumber());

        // 12. Agency Performance Report
        mockMvc.perform(get("/api/v1/agency/reports/performance")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalJobsAssigned").isNumber())
                .andExpect(jsonPath("$.data.qcPassPercentage").isNumber())
                .andExpect(jsonPath("$.data.totalEarnings").isNumber());
    }
}
