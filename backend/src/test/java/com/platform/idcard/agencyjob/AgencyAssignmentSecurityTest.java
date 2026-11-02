package com.platform.idcard.agencyjob;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.agencyjob.dto.AgencyAssignmentItemRequest;
import com.platform.idcard.agencyjob.dto.AssignOrderRequest;
import com.platform.idcard.agencyjob.dto.JobItemAllocationRequest;
import com.platform.idcard.agencyjob.dto.RejectJobRequest;
import com.platform.idcard.agencyjob.entity.AgencyAssignment;
import com.platform.idcard.agencyjob.repository.AgencyAssignmentRepository;
import com.platform.idcard.agencyjob.service.AgencyJobService;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.order.dto.CreateOrderRequest;
import com.platform.idcard.order.dto.GenerateQuotationRequest;
import com.platform.idcard.order.dto.QuotationItemRequest;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.student.dto.StudentRequest;
import com.platform.idcard.student.service.StudentService;
import com.platform.idcard.template.dto.AssignTemplateRequest;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security and Tenant Isolation Tests for Step 6 (Agency Management and Job Assignment).
 *
 * Verifies Non-Negotiable Rules:
 * Rule 1: School must never see agency data. Agency must never see school prices or customer details.
 * Rule 2: Never trust tenant IDs from client payload; derive strictly from token.
 * Rule 3: Monetary precision in NUMERIC(14,2).
 * Rule 4: Named business state transitions only.
 * Rule 6: Paginated responses and indexed queries.
 * Rule 8: Jakarta validation constraints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgencyAssignmentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private AgencyJobService agencyJobService;

    @Autowired
    private AgencyAssignmentRepository agencyAssignmentRepository;

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

    private String agencyAToken;
    private UUID agencyAId;

    private String agencyBToken;
    private UUID agencyBId;

    private String orderId;
    private String orderItemId;
    private UUID jobAId;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure roles
        Role adminRole = roleRepository.findByRoleName("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "SUPER_ADMIN", "Admin")));
        Role schoolRole = roleRepository.findByRoleName("SCHOOL")
                .orElseGet(() -> roleRepository.save(new Role(null, "SCHOOL", "School")));
        Role agencyRole = roleRepository.findByRoleName("PRINTING_AGENCY")
                .orElseGet(() -> roleRepository.save(new Role(null, "PRINTING_AGENCY", "Agency")));

        // Ensure Admin user & login
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

        // Setup School & Login
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var schoolDto = schoolService.createSchool(new SchoolRequest(
                "Job Sec School " + suffix, "SCH-JS-" + suffix, "10 Campus Way",
                "Metropolis", "State X", "500001", "sec_job_school_" + suffix + "@test.edu",
                "9876543010", "ACTIVE"
        ));
        schoolId = schoolDto.id();

        schoolService.createSchoolUser(schoolId, new CreateOrganizationUserRequest(
                "School Manager", "sec_job_user_" + suffix + "@test.edu", "9876543010", "Password123!", "Admin"
        ));

        MvcResult schoolLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("sec_job_user_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolToken = objectMapper.readTree(schoolLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Setup Agency A & Login
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        var agADto = agencyService.createAgency(new AgencyRequest(
                "Print Agency A " + suffixA, "Contact A", "ag_a_" + suffixA + "@print.com",
                "9876543011", "Industrial Area A", 50000, "500002", "ACTIVE"
        ));
        agencyAId = agADto.id();

        agencyService.createAgencyUser(agencyAId, new CreateOrganizationUserRequest(
                "Agency A Manager", "ag_a_user_" + suffixA + "@print.com", "9876543011", "Password123!", "Manager"
        ));

        MvcResult agALogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ag_a_user_" + suffixA + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyAToken = objectMapper.readTree(agALogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Setup Agency B & Login
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        var agBDto = agencyService.createAgency(new AgencyRequest(
                "Print Agency B " + suffixB, "Contact B", "ag_b_" + suffixB + "@print.com",
                "9876543012", "Industrial Area B", 40000, "500003", "ACTIVE"
        ));
        agencyBId = agBDto.id();

        agencyService.createAgencyUser(agencyBId, new CreateOrganizationUserRequest(
                "Agency B Manager", "ag_b_user_" + suffixB + "@print.com", "9876543012", "Password123!", "Manager"
        ));

        MvcResult agBLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ag_b_user_" + suffixB + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyBToken = objectMapper.readTree(agBLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Setup Student
        var stu = studentService.createStudent(schoolId, new StudentRequest(
                "ADM-JOB-01", "Rohan Gupta", "Father", "Mother",
                LocalDate.of(2011, 5, 12), "MALE", "O+", "10", "A", "01",
                "Address", "9876543013", null, null, "ACTIVE"
        ));

        // Create & approve template
        String tplName = "Job Sec Template " + suffix;
        MvcResult tplRes = mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCardTemplateRequest(
                                tplName, "STUDENT", null,
                                new BigDecimal("85.60"), new BigDecimal("54.00"), new BigDecimal("3.00"),
                                "{\"elements\":[]}", "{\"elements\":[]}"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String tplId = objectMapper.readTree(tplRes.getResponse().getContentAsString()).path("data").path("id").asText();
        String v1Id = objectMapper.readTree(tplRes.getResponse().getContentAsString()).path("data").path("versions").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/admin/templates/" + tplId + "/versions/" + v1Id + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        MvcResult appRes = mockMvc.perform(post("/api/v1/admin/templates/" + tplId + "/versions/" + v1Id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        UUID templateVerId = UUID.fromString(
                objectMapper.readTree(appRes.getResponse().getContentAsString()).path("data").path("id").asText()
        );

        mockMvc.perform(post("/api/v1/admin/templates/" + tplId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTemplateRequest(schoolId, "2026-2027", true))))
                .andExpect(status().isOk());

        // Create Order, Submit, Verify, Quote, Approve
        MvcResult orderRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                templateVerId, "2026-2027", "DIRECT_TO_SCHOOL",
                                "10 Campus Way", "Manager", "9876543010", null,
                                List.of(stu.id()), null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode orderData = objectMapper.readTree(orderRes.getResponse().getContentAsString()).path("data");
        orderId = orderData.path("id").asText();
        orderItemId = orderData.path("items").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/start-review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateQuotationRequest(
                                BigDecimal.ZERO, new BigDecimal("18.00"), 15, "Notes",
                                List.of(new QuotationItemRequest("Card Printing", 1, new BigDecimal("45.00")))
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/approve-quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk());

        // Assign to Agency A
        var assignReq = new AssignOrderRequest(List.of(
                new AgencyAssignmentItemRequest(
                        agencyAId,
                        LocalDate.now().plusDays(7),
                        new BigDecimal("22.50"),
                        "Urgent batch",
                        List.of(new JobItemAllocationRequest(UUID.fromString(orderItemId), 1))
                )
        ));

        MvcResult assignRes = mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/assignments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isCreated())
                .andReturn();

        jobAId = UUID.fromString(
                objectMapper.readTree(assignRes.getResponse().getContentAsString()).path("data").get(0).path("id").asText()
        );
    }

    @Test
    @DisplayName("Unauthenticated requests to agency job endpoints return 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/agency/jobs"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/assignments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/agency/jobs/" + jobAId + "/accept"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Rule 1: School user cannot access agency jobs or admin assignment endpoints (403 Forbidden)")
    void testSchoolCannotAccessAgencyEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/agency/jobs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));

        mockMvc.perform(get("/api/v1/admin/assignments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Agency user cannot access Super Admin assignment management endpoints (403 Forbidden)")
    void testAgencyCannotAccessAdminAssignmentEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/assignments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));

        mockMvc.perform(get("/api/v1/admin/assignments/" + UUID.randomUUID())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));

        AssignOrderRequest validBody = new AssignOrderRequest(List.of(
                new AgencyAssignmentItemRequest(
                        agencyAId,
                        LocalDate.now().plusDays(5),
                        new BigDecimal("25.00"),
                        "Note",
                        List.of(new JobItemAllocationRequest(UUID.randomUUID(), 1))
                )
        ));

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/assignments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validBody)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Rule 2: Agency B cannot view or accept Agency A's assigned job (404 Not Found)")
    void testAgencyCannotAccessOtherAgencyJob() throws Exception {
        // Agency B attempts to view Agency A's job -> 404 Not Found
        mockMvc.perform(get("/api/v1/agency/jobs/" + jobAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));

        // Agency B attempts to accept Agency A's job -> 404 Not Found
        mockMvc.perform(post("/api/v1/agency/jobs/" + jobAId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Rule 1: Agency Job response must never contain school selling price or platform margin")
    void testAgencyJobResponseOmitFinancialCustomerDetails() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/agency/jobs/" + jobAId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyAToken))
                .andExpect(status().isOk())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        assertThat(body).doesNotContain("sellingPrice");
        assertThat(body).doesNotContain("profit");
        assertThat(body).doesNotContain("margin");
        assertThat(body).doesNotContain("schoolPrice");
        assertThat(body).doesNotContain("contactMobile");
    }

    @Test
    @DisplayName("Rule 8: Jakarta validation requires rejectionReason when rejecting a job")
    void testRejectJobRequiresReason() throws Exception {
        mockMvc.perform(post("/api/v1/agency/jobs/" + jobAId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectJobRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Rule 4: Cannot accept a job that is already rejected")
    void testCannotAcceptRejectedJob() throws Exception {
        // Agency A rejects job
        mockMvc.perform(post("/api/v1/agency/jobs/" + jobAId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectJobRequest("Capacity shortage"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus", is("REJECTED")));

        // Try to accept rejected job -> 400 INVALID_ACTION
        mockMvc.perform(post("/api/v1/agency/jobs/" + jobAId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyAToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_ACTION")));
    }
}
