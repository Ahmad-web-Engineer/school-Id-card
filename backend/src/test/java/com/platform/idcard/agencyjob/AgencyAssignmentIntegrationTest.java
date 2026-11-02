package com.platform.idcard.agencyjob;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.agencyjob.dto.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AgencyAssignmentIntegrationTest {

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
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private String schoolToken;
    private UUID schoolId;

    private String agency1Token;
    private UUID agency1Id;

    private String agency2Token;
    private UUID agency2Id;

    private String agency3Token;
    private UUID agency3Id;

    private UUID approvedTemplateVersionId;
    private List<UUID> studentIds;

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

        MvcResult adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@platform.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Setup School
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var schoolDto = schoolService.createSchool(new SchoolRequest(
                "Lifecycle Academy " + suffix, "SCH-LC-" + suffix, "100 Campus Blvd",
                "Metropolis", "State X", "500001", "lc_school_" + suffix + "@test.edu",
                "9876543020", "ACTIVE"
        ));
        schoolId = schoolDto.id();

        schoolService.createSchoolUser(schoolId, new CreateOrganizationUserRequest(
                "School Coordinator", "lc_user_" + suffix + "@test.edu", "9876543020", "Password123!", "Admin"
        ));

        MvcResult schoolLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("lc_user_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolToken = objectMapper.readTree(schoolLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Setup 3 Agencies
        var ag1 = agencyService.createAgency(new AgencyRequest(
                "Agency One " + suffix, "Manager 1", "ag1_" + suffix + "@print.com",
                "9876543021", "Zone 1", 30000, "500002", "ACTIVE"
        ));
        agency1Id = ag1.id();
        agencyService.createAgencyUser(agency1Id, new CreateOrganizationUserRequest(
                "Ag1 Lead", "ag1_lead_" + suffix + "@print.com", "9876543021", "Password123!", "Lead"
        ));
        MvcResult ag1Login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ag1_lead_" + suffix + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agency1Token = objectMapper.readTree(ag1Login.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var ag2 = agencyService.createAgency(new AgencyRequest(
                "Agency Two " + suffix, "Manager 2", "ag2_" + suffix + "@print.com",
                "9876543022", "Zone 2", 25000, "500003", "ACTIVE"
        ));
        agency2Id = ag2.id();
        agencyService.createAgencyUser(agency2Id, new CreateOrganizationUserRequest(
                "Ag2 Lead", "ag2_lead_" + suffix + "@print.com", "9876543022", "Password123!", "Lead"
        ));
        MvcResult ag2Login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ag2_lead_" + suffix + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agency2Token = objectMapper.readTree(ag2Login.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var ag3 = agencyService.createAgency(new AgencyRequest(
                "Agency Three " + suffix, "Manager 3", "ag3_" + suffix + "@print.com",
                "9876543023", "Zone 3", 45000, "500004", "ACTIVE"
        ));
        agency3Id = ag3.id();
        agencyService.createAgencyUser(agency3Id, new CreateOrganizationUserRequest(
                "Ag3 Lead", "ag3_lead_" + suffix + "@print.com", "9876543023", "Password123!", "Lead"
        ));
        MvcResult ag3Login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ag3_lead_" + suffix + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agency3Token = objectMapper.readTree(ag3Login.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Setup 5 Students in School
        studentIds = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            var s = studentService.createStudent(schoolId, new StudentRequest(
                    "ADM-LC-" + i, "Student " + i, "Father", "Mother",
                    LocalDate.of(2011, i, 10), "MALE", "O+", "10", "A", String.format("%02d", i),
                    "City", "9876543020", null, null, "ACTIVE"
            ));
            studentIds.add(s.id());
        }

        // Setup Template
        MvcResult tplRes = mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCardTemplateRequest(
                                "Lifecycle Template " + suffix, "STUDENT", null,
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
        approvedTemplateVersionId = UUID.fromString(
                objectMapper.readTree(appRes.getResponse().getContentAsString()).path("data").path("id").asText()
        );

        mockMvc.perform(post("/api/v1/admin/templates/" + tplId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTemplateRequest(schoolId, "2026-2027", true))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Complete Order Splitting, Reassignment, and Agency Acceptance Lifecycle")
    void testCompleteAgencyJobLifecycle() throws Exception {
        // 1. School creates order with 5 students
        MvcResult orderRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                approvedTemplateVersionId, "2026-2027", "DIRECT_TO_SCHOOL",
                                "100 Campus Blvd", "Principal", "9876543020", null,
                                studentIds, null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode orderData = objectMapper.readTree(orderRes.getResponse().getContentAsString()).path("data");
        String orderId = orderData.path("id").asText();
        String orderItemId = orderData.path("items").get(0).path("id").asText();

        // 2. Submit order
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk());

        // 3. Admin review & verify
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/start-review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // 4. Admin generate quotation
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateQuotationRequest(
                                BigDecimal.ZERO, new BigDecimal("18.00"), 15, "Notes",
                                List.of(new QuotationItemRequest("PVC ID Cards", 5, new BigDecimal("50.00")))
                        ))))
                .andExpect(status().isCreated());

        // 5. School approves quotation -> Status APPROVED
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/approve-quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("APPROVED")));

        // 6. Super Admin splits order across Agency 1 (3 cards @ 25.00) and Agency 2 (2 cards @ 24.00)
        AssignOrderRequest splitReq = new AssignOrderRequest(List.of(
                new AgencyAssignmentItemRequest(
                        agency1Id,
                        LocalDate.now().plusDays(5),
                        new BigDecimal("25.00"),
                        "Primary batch",
                        List.of(new JobItemAllocationRequest(UUID.fromString(orderItemId), 3))
                ),
                new AgencyAssignmentItemRequest(
                        agency2Id,
                        LocalDate.now().plusDays(7),
                        new BigDecimal("24.00"),
                        "Secondary batch",
                        List.of(new JobItemAllocationRequest(UUID.fromString(orderItemId), 2))
                )
        ));

        MvcResult assignRes = mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/assignments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(splitReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andReturn();

        JsonNode assignData = objectMapper.readTree(assignRes.getResponse().getContentAsString()).path("data");
        String job1Id = assignData.get(0).path("id").asText();
        String job2Id = assignData.get(1).path("id").asText();

        // Verify parent order status is now ASSIGNED
        mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("ASSIGNED")));

        // 7. Agency 1 accepts its job
        mockMvc.perform(post("/api/v1/agency/jobs/" + job1Id + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agency1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus", is("AGENCY_ACCEPTED")));

        // 8. Agency 2 rejects its job with reason
        mockMvc.perform(post("/api/v1/agency/jobs/" + job2Id + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agency2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectJobRequest("Machine calibration issue"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus", is("REJECTED")));

        // Parent order status returns to AWAITING_ASSIGNMENT because a portion is rejected
        mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("AWAITING_ASSIGNMENT")));

        // 9. Admin reassigns Agency 2's job to Agency 3 (@ 26.00)
        ReassignJobRequest reassignReq = new ReassignJobRequest(
                agency3Id,
                LocalDate.now().plusDays(6),
                new BigDecimal("26.00"),
                "Reallocated from Agency 2 due to machine calibration"
        );

        MvcResult reassignRes = mockMvc.perform(post("/api/v1/admin/assignments/" + job2Id + "/reassign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reassignReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus", is("ASSIGNED")))
                .andExpect(jsonPath("$.data.reassignedFromId", is(job2Id)))
                .andExpect(jsonPath("$.data.totalAssignedQuantity", is(2)))
                .andExpect(jsonPath("$.data.agreedRate", is(26.0)))
                .andExpect(jsonPath("$.data.totalAgreedAmount", is(52.0)))
                .andReturn();

        String job3Id = objectMapper.readTree(reassignRes.getResponse().getContentAsString()).path("data").path("id").asText();

        // 10. Agency 3 accepts job -> all active jobs (Job 1 & Job 3) are now AGENCY_ACCEPTED
        mockMvc.perform(post("/api/v1/agency/jobs/" + job3Id + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agency3Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus", is("AGENCY_ACCEPTED")));

        // Parent order status moves automatically to AGENCY_ACCEPTED!
        mockMvc.perform(get("/api/v1/admin/orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("AGENCY_ACCEPTED")));
    }

    @Test
    @DisplayName("Validation: Over-allocating quantity beyond order item fails with 400 ValidationException")
    void testOverAllocationRejection() throws Exception {
        // Setup order with 1 card
        MvcResult orderRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                approvedTemplateVersionId, "2026-2027", "DIRECT_TO_SCHOOL",
                                "100 Campus Blvd", "Principal", "9876543020", null,
                                List.of(studentIds.get(0)), null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String oId = objectMapper.readTree(orderRes.getResponse().getContentAsString()).path("data").path("id").asText();
        String oiId = objectMapper.readTree(orderRes.getResponse().getContentAsString()).path("data").path("items").get(0).path("id").asText();

        // Submit -> Verify -> Quote -> Approve
        mockMvc.perform(post("/api/v1/school/orders/" + oId + "/submit").header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/" + oId + "/start-review").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/" + oId + "/verify").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/" + oId + "/quotation").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new GenerateQuotationRequest(BigDecimal.ZERO, new BigDecimal("18.00"), 15, "Notes", List.of(new QuotationItemRequest("Card", 1, new BigDecimal("45.00"))))))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/school/orders/" + oId + "/approve-quotation").header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)).andExpect(status().isOk());

        // Try to allocate 5 cards when order only has 1 card
        AssignOrderRequest overAllocReq = new AssignOrderRequest(List.of(
                new AgencyAssignmentItemRequest(
                        agency1Id,
                        LocalDate.now().plusDays(5),
                        new BigDecimal("25.00"),
                        "Over allocation",
                        List.of(new JobItemAllocationRequest(UUID.fromString(oiId), 5)) // 5 > 1!
                )
        ));

        mockMvc.perform(post("/api/v1/admin/orders/" + oId + "/assignments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overAllocReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.message", containsString("exceeds total order item quantity")));
    }
}
