package com.platform.idcard.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.agencyjob.dto.AgencyAssignmentItemRequest;
import com.platform.idcard.agencyjob.dto.AssignOrderRequest;
import com.platform.idcard.agencyjob.dto.JobItemAllocationRequest;
import com.platform.idcard.agencyjob.service.AgencyJobService;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.delivery.dto.ConfirmDeliveryRequest;
import com.platform.idcard.delivery.dto.CreateDispatchPayload;
import com.platform.idcard.finance.dto.GenerateInvoiceRequest;
import com.platform.idcard.finance.dto.GenerateSettlementRequest;
import com.platform.idcard.finance.dto.PaySettlementRequest;
import com.platform.idcard.finance.dto.RecordPaymentRequest;
import com.platform.idcard.finance.entity.PaymentMethod;
import com.platform.idcard.order.dto.CreateOrderRequest;
import com.platform.idcard.order.dto.GenerateQuotationRequest;
import com.platform.idcard.order.dto.QuotationItemRequest;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.production.dto.CreateBatchRequest;
import com.platform.idcard.production.dto.QualityCheckRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FullPlatformE2EHardeningTest {

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
                "Hardening School " + suffix, "SCH-HRD-" + suffix, "100 Security Blvd",
                "Metro City", "State", "500001", "hrd_sch_" + suffix + "@test.edu",
                "9876543990", "ACTIVE"
        ));
        schoolId = sDto.id();

        var sUser = schoolService.createSchoolUser(schoolId, new CreateOrganizationUserRequest(
                "Hardening School Admin", "hrd_usr_" + suffix + "@test.edu", "9876543990", "Password123!", "Admin"
        ));
        schoolUserId = sUser.id();

        MvcResult sLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("hrd_usr_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolToken = objectMapper.readTree(sLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var agDto = agencyService.createAgency(new AgencyRequest(
                "Hardening Agency " + suffix, "Lead Contact", "hrd_ag_" + suffix + "@print.com",
                "9876543991", "200 Print Ave", 40000, "500002", "ACTIVE"
        ));
        agencyId = agDto.id();

        var agUser = agencyService.createAgencyUser(agencyId, new CreateOrganizationUserRequest(
                "Hardening Agency Manager", "hrd_agu_" + suffix + "@print.com", "9876543991", "Password123!", "Manager"
        ));
        agencyUserId = agUser.id();

        MvcResult agLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("hrd_agu_" + suffix + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(agLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Complete E2E Hardening Journey: Full Lifecycle across Admin, School, Agency with Zero Leakage")
    void testCompletePlatformLifecycleE2E() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. School registers student
        var student = studentService.createStudent(schoolId, new StudentRequest(
                "ADM-E2E-01", "Aarav Patel", "Father", "Mother",
                LocalDate.of(2011, 4, 10), "MALE", "B+", "9", "A", "12",
                "Address Line 1", "9876543992", null, null, "ACTIVE"
        ));

        // 2. Admin creates & approves Card Template
        String tplName = "E2E Hardened Template " + suffix;
        MvcResult tplRes = mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCardTemplateRequest(
                                tplName, "STUDENT", null,
                                new BigDecimal("100.00"), new BigDecimal("50.00"), new BigDecimal("5.00"),
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

        // 3. School places order
        MvcResult orderRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                templateVerId, "2026-2027", "DIRECT_TO_SCHOOL",
                                "100 Security Blvd", "Hardening Admin", "9876543990", null,
                                List.of(student.id()), null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode orderData = objectMapper.readTree(orderRes.getResponse().getContentAsString()).path("data");
        UUID orderId = UUID.fromString(orderData.path("id").asText());
        UUID orderItemId = UUID.fromString(orderData.path("items").get(0).path("id").asText());

        // 4. Submit order
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk());

        // 5. Admin reviews, verifies, and generates quotation
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
                                BigDecimal.ZERO, new BigDecimal("18.00"), 15, "Official Quotation",
                                List.of(new QuotationItemRequest("Smart Card", 1, new BigDecimal("100.00")))
                        ))))
                .andExpect(status().isCreated());

        // 6. School approves quotation
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/approve-quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk());

        // 7. Admin assigns order to Agency
        var assignments = agencyJobService.assignOrderToAgencies(orderId, adminUserId, new AssignOrderRequest(
                List.of(new AgencyAssignmentItemRequest(
                        agencyId,
                        LocalDate.now().plusDays(7),
                        new BigDecimal("45.00"),
                        "Standard turnaround",
                        List.of(new JobItemAllocationRequest(orderItemId, 1))
                ))
        ));
        UUID assignmentId = assignments.get(0).id();

        // 8. Agency accepts assignment
        mockMvc.perform(post("/api/v1/agency/jobs/" + assignmentId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isOk());

        // 9. Agency starts production & creates batch
        mockMvc.perform(post("/api/v1/agency/jobs/" + assignmentId + "/batches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBatchRequest(1, "Batch notes"))))
                .andExpect(status().isCreated());

        // 10. Agency submits quality check: 1 inspected, 1 accepted, 0 rejected
        mockMvc.perform(post("/api/v1/agency/jobs/" + assignmentId + "/quality-checks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QualityCheckRequest(
                                1, 1, 0, null, null, "No visual defects, RFID verified"
                        ))))
                .andExpect(status().isCreated());

        // 11. Agency dispatches parcel (Model A: DIRECT_TO_SCHOOL)
        mockMvc.perform(post("/api/v1/agency/jobs/" + assignmentId + "/dispatch")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDispatchPayload(
                                "BlueDart", "BD-889912", 1, "Delivering to school reception", null
                        ))))
                .andExpect(status().isCreated());

        // 12. School confirms delivery receipt
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/confirm-delivery")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmDeliveryRequest(
                                1, "Hardening Admin", "9876543990", "Received intact", null
                        ))))
                .andExpect(status().isCreated());

        // 13. Admin generates tax invoice: 100 subtotal + 18 tax = 118 total
        MvcResult invRes = mockMvc.perform(post("/api/v1/admin/finance/orders/" + orderId + "/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateInvoiceRequest(
                                LocalDate.now().plusDays(30), "Tax invoice"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID invoiceId = UUID.fromString(
                objectMapper.readTree(invRes.getResponse().getContentAsString()).path("data").path("id").asText()
        );

        // 14. School pays full balance (118.00)
        mockMvc.perform(post("/api/v1/school/finance/orders/" + orderId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordPaymentRequest(
                                new BigDecimal("118.00"), PaymentMethod.UPI, "UPI/SETTLE-118", "Full payment"
                        ))))
                .andExpect(status().isCreated());

        // 15. Verify Invoice is marked PAID
        mockMvc.perform(get("/api/v1/school/finance/invoices/" + invoiceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.invoiceStatus").value("PAID"))
                .andExpect(jsonPath("$.data.dueAmount").value(0.00));

        // 16. Admin generates agency settlement & pays agency (45.00)
        MvcResult setRes = mockMvc.perform(post("/api/v1/admin/finance/assignments/" + assignmentId + "/settlement")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateSettlementRequest(
                                BigDecimal.ZERO, BigDecimal.ZERO, "Full job payout"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        UUID settlementId = UUID.fromString(
                objectMapper.readTree(setRes.getResponse().getContentAsString()).path("data").path("id").asText()
        );

        mockMvc.perform(post("/api/v1/admin/finance/settlements/" + settlementId + "/pay")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaySettlementRequest(
                                new BigDecimal("45.00"), "NEFT/AGY-45", "Disbursed"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.settlementStatus").value("SETTLED"));

        // 17. Verify Strict Multi-Tenant Isolation & Role Walls (Rule 1 Non-negotiable):
        // School tracking endpoint never mentions agency name, agency contact, internal job number, or agency cost
        mockMvc.perform(get("/api/v1/school/orders/" + orderId + "/tracking")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.agencyName").doesNotExist())
                .andExpect(jsonPath("$.data.agencyId").doesNotExist())
                .andExpect(jsonPath("$.data.agreedRate").doesNotExist());

        // Agency assignment endpoint never mentions school prices or platform margins
        mockMvc.perform(get("/api/v1/agency/jobs/" + assignmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schoolSellingPrice").doesNotExist())
                .andExpect(jsonPath("$.data.quotationAmount").doesNotExist());
    }

    @Test
    @DisplayName("Penetration & Negative Security Matrix: Validation & Permission Tampering")
    void testSecurityPenetrationAndTamperingRejection() throws Exception {
        // 1. Negative amount payment injection -> 400
        mockMvc.perform(post("/api/v1/school/finance/orders/" + UUID.randomUUID() + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordPaymentRequest(
                                new BigDecimal("-50.00"), PaymentMethod.UPI, "FAKE", "Exploit attempt"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));

        // 2. School attempting to access Admin Audit Logs -> 403
        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("ACCESS_DENIED"));

        // 3. Agency attempting to view School Invoices -> 403
        mockMvc.perform(get("/api/v1/school/finance/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden());

        // 4. Missing Authorization Header -> 401
        mockMvc.perform(get("/api/v1/admin/reports/operational"))
                .andExpect(status().isUnauthorized());
    }
}
