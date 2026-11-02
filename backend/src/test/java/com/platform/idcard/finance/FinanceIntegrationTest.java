package com.platform.idcard.finance;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanceIntegrationTest {

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
    private String agencyToken;
    private UUID agencyId;
    private UUID orderId;
    private UUID orderItemId;
    private UUID assignmentId;

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
        var schoolDto = schoolService.createSchool(new SchoolRequest(
                "Finance Test School " + suffix, "SCH-FT-" + suffix, "123 School Road",
                "Metropolis", "State", "500001", "fin_int_sch_" + suffix + "@test.edu",
                "9876543050", "ACTIVE"
        ));
        schoolId = schoolDto.id();

        schoolService.createSchoolUser(schoolId, new CreateOrganizationUserRequest(
                "School Fin Lead", "fin_int_lead_" + suffix + "@test.edu", "9876543050", "Password123!", "Admin"
        ));

        MvcResult schoolLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("fin_int_lead_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolToken = objectMapper.readTree(schoolLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var agencyDto = agencyService.createAgency(new AgencyRequest(
                "Finance Test Agency " + suffix, "Partner Lead", "fin_int_ag_" + suffix + "@print.com",
                "9876543051", "456 Print Way", 30000, "500002", "ACTIVE"
        ));
        agencyId = agencyDto.id();

        agencyService.createAgencyUser(agencyId, new CreateOrganizationUserRequest(
                "Agency Fin Officer", "fin_int_agusr_" + suffix + "@print.com", "9876543051", "Password123!", "Manager"
        ));

        MvcResult agencyLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("fin_int_agusr_" + suffix + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(agencyLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var stu = studentService.createStudent(schoolId, new StudentRequest(
                "ADM-FINT-01", "Ananya Sharma", "Rajesh Sharma", "Pooja Sharma",
                LocalDate.of(2012, 3, 15), "FEMALE", "O+", "8", "B", "14",
                "Residential Colony", "9876543052", null, null, "ACTIVE"
        ));

        String tplName = "Fin Integration Template " + suffix;
        MvcResult tplRes = mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCardTemplateRequest(
                                tplName, "STUDENT", null,
                                new BigDecimal("120.00"), new BigDecimal("60.00"), new BigDecimal("5.00"),
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

        MvcResult orderRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                templateVerId, "2026-2027", "DIRECT_TO_SCHOOL",
                                "123 School Road", "School Fin Lead", "9876543050", null,
                                List.of(stu.id()), null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode orderData = objectMapper.readTree(orderRes.getResponse().getContentAsString()).path("data");
        orderId = UUID.fromString(orderData.path("id").asText());
        orderItemId = UUID.fromString(orderData.path("items").get(0).path("id").asText());

        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/start-review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Quotation: 1 card @ 100.00 subtotal, tax 18% = 18.00, total = 118.00
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateQuotationRequest(
                                BigDecimal.ZERO, new BigDecimal("18.00"), 15, "Standard Terms",
                                List.of(new QuotationItemRequest("PVC Smart ID Card", 1, new BigDecimal("100.00")))
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/approve-quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk());

        // Assign to agency
        var assignments = agencyJobService.assignOrderToAgencies(orderId, adminUserId, new AssignOrderRequest(
                List.of(new AgencyAssignmentItemRequest(
                        agencyId,
                        LocalDate.now().plusDays(10),
                        new BigDecimal("50.00"),
                        "Assignment Terms",
                        List.of(new JobItemAllocationRequest(orderItemId, 1))
                ))
        ));
        assignmentId = assignments.get(0).id();

        mockMvc.perform(post("/api/v1/agency/jobs/" + assignmentId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("End-to-end Finance Flow: Invoice Generation -> Partial & Full Payment -> Settlement Generation -> Payout -> Finance P&L Summary")
    void testEndToEndFinanceLifecycle() throws Exception {
        // 1. Admin generates invoice for order
        MvcResult invRes = mockMvc.perform(post("/api/v1/admin/finance/orders/" + orderId + "/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateInvoiceRequest(
                                LocalDate.now().plusDays(30),
                                "Invoice for 2026-2027 ID Cards batch"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalAmount").value(118.00))
                .andExpect(jsonPath("$.data.paidAmount").value(0.00))
                .andExpect(jsonPath("$.data.dueAmount").value(118.00))
                .andExpect(jsonPath("$.data.invoiceStatus").value("ISSUED"))
                .andReturn();

        UUID invoiceId = UUID.fromString(
                objectMapper.readTree(invRes.getResponse().getContentAsString()).path("data").path("id").asText()
        );

        // 2. School lists invoices
        mockMvc.perform(get("/api/v1/school/finance/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.data[0].id").value(invoiceId.toString()))
                .andExpect(jsonPath("$.data.data[0].totalAmount").value(118.00))
                .andExpect(jsonPath("$.data.data[0].invoiceStatus").value("ISSUED"));

        // 3. School records partial payment: 50.00
        mockMvc.perform(post("/api/v1/school/finance/orders/" + orderId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordPaymentRequest(
                                new BigDecimal("50.00"),
                                PaymentMethod.UPI,
                                "UPI/REF-12345",
                                "Advance 50 payment"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(50.00))
                .andExpect(jsonPath("$.data.paymentStatus").value("COMPLETED"));

        // 4. Verify Invoice is now PARTIALLY_PAID (paid 50.00, due 68.00)
        mockMvc.perform(get("/api/v1/school/finance/invoices/" + invoiceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paidAmount").value(50.00))
                .andExpect(jsonPath("$.data.dueAmount").value(68.00))
                .andExpect(jsonPath("$.data.invoiceStatus").value("PARTIALLY_PAID"));

        // 5. School records remaining payment: 68.00
        mockMvc.perform(post("/api/v1/school/finance/orders/" + orderId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RecordPaymentRequest(
                                new BigDecimal("68.00"),
                                PaymentMethod.BANK_TRANSFER,
                                "NEFT/REF-98765",
                                "Final payment for batch"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.amount").value(68.00));

        // 6. Verify Invoice is now PAID (paid 118.00, due 0.00)
        mockMvc.perform(get("/api/v1/school/finance/invoices/" + invoiceId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidAmount").value(118.00))
                .andExpect(jsonPath("$.data.dueAmount").value(0.00))
                .andExpect(jsonPath("$.data.invoiceStatus").value("PAID"));

        // 7. Verify School Finance Summary reflects zero outstanding balance
        mockMvc.perform(get("/api/v1/school/finance/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalInvoiced").value(118.00))
                .andExpect(jsonPath("$.data.totalPaid").value(118.00))
                .andExpect(jsonPath("$.data.outstandingBalance").value(0.00));

        // 8. Admin generates Agency Settlement: agreed 50.00, penalty 5.00 -> payable 45.00
        MvcResult setRes = mockMvc.perform(post("/api/v1/admin/finance/assignments/" + assignmentId + "/settlement")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateSettlementRequest(
                                BigDecimal.ZERO,
                                new BigDecimal("5.00"),
                                "Minor delivery packaging delay penalty"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.agreedAmount").value(50.00))
                .andExpect(jsonPath("$.data.penaltyAmount").value(5.00))
                .andExpect(jsonPath("$.data.payableAmount").value(45.00))
                .andExpect(jsonPath("$.data.outstandingAmount").value(45.00))
                .andExpect(jsonPath("$.data.settlementStatus").value("APPROVED"))
                .andReturn();

        UUID settlementId = UUID.fromString(
                objectMapper.readTree(setRes.getResponse().getContentAsString()).path("data").path("id").asText()
        );

        // 9. Agency views Settlement
        mockMvc.perform(get("/api/v1/agency/finance/settlements/" + settlementId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.payableAmount").value(45.00))
                .andExpect(jsonPath("$.data.outstandingAmount").value(45.00));

        // 10. Agency Finance Summary shows 45.00 pending payout
        mockMvc.perform(get("/api/v1/agency/finance/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAgreedAmount").value(50.00))
                .andExpect(jsonPath("$.data.totalPayableAmount").value(45.00))
                .andExpect(jsonPath("$.data.totalPaidAmount").value(0.00))
                .andExpect(jsonPath("$.data.outstandingPayable").value(45.00));

        // 11. Admin pays agency settlement
        mockMvc.perform(post("/api/v1/admin/finance/settlements/" + settlementId + "/pay")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PaySettlementRequest(
                                new BigDecimal("45.00"),
                                "TXN-AGY-SETTLE-001",
                                "Disbursed via bank transfer"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paidAmount").value(45.00))
                .andExpect(jsonPath("$.data.outstandingAmount").value(0.00))
                .andExpect(jsonPath("$.data.settlementStatus").value("SETTLED"));

        // 12. Verify Admin Finance Overview P&L
        mockMvc.perform(get("/api/v1/admin/finance/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.grossRevenue").value(118.00))
                .andExpect(jsonPath("$.data.agencyDirectCost").value(45.00))
                .andExpect(jsonPath("$.data.grossContribution").value(73.00));
    }
}
