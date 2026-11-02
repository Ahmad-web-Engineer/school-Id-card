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
import com.platform.idcard.finance.dto.RecordPaymentRequest;
import com.platform.idcard.finance.entity.PaymentMethod;
import com.platform.idcard.finance.service.FinanceService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinanceSecurityTest {

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
    private FinanceService financeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID adminUserId;
    private String school1Token;
    private String school2Token;
    private String agency1Token;
    private String agency2Token;

    private UUID school1Id;
    private UUID school2Id;
    private UUID agency1Id;
    private UUID agency2Id;

    private UUID orderId;
    private UUID assignment1Id;

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
                "School Fin 1 " + suffix, "SCH-F1-" + suffix, "10 Fin Way",
                "Metropolis", "State X", "500001", "fin_sch1_" + suffix + "@test.edu",
                "9876543010", "ACTIVE"
        ));
        school1Id = s1Dto.id();

        schoolService.createSchoolUser(school1Id, new CreateOrganizationUserRequest(
                "School 1 Manager", "fin_usr1_" + suffix + "@test.edu", "9876543010", "Password123!", "Admin"
        ));

        MvcResult s1Login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("fin_usr1_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        school1Token = objectMapper.readTree(s1Login.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var s2Dto = schoolService.createSchool(new SchoolRequest(
                "School Fin 2 " + suffix, "SCH-F2-" + suffix, "20 Fin Way",
                "Metropolis", "State X", "500001", "fin_sch2_" + suffix + "@test.edu",
                "9876543020", "ACTIVE"
        ));
        school2Id = s2Dto.id();

        schoolService.createSchoolUser(school2Id, new CreateOrganizationUserRequest(
                "School 2 Manager", "fin_usr2_" + suffix + "@test.edu", "9876543020", "Password123!", "Admin"
        ));

        MvcResult s2Login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("fin_usr2_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        school2Token = objectMapper.readTree(s2Login.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        var ag1Dto = agencyService.createAgency(new AgencyRequest(
                "Agency Fin 1 " + suffixA, "Contact 1", "fin_ag1_" + suffixA + "@print.com",
                "9876543011", "Industrial Area 1", 50000, "500002", "ACTIVE"
        ));
        agency1Id = ag1Dto.id();

        agencyService.createAgencyUser(agency1Id, new CreateOrganizationUserRequest(
                "Agency 1 Manager", "fin_agu1_" + suffixA + "@print.com", "9876543011", "Password123!", "Manager"
        ));

        MvcResult ag1Login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("fin_agu1_" + suffixA + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agency1Token = objectMapper.readTree(ag1Login.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        var ag2Dto = agencyService.createAgency(new AgencyRequest(
                "Agency Fin 2 " + suffixB, "Contact 2", "fin_ag2_" + suffixB + "@print.com",
                "9876543012", "Industrial Area 2", 40000, "500003", "ACTIVE"
        ));
        agency2Id = ag2Dto.id();

        agencyService.createAgencyUser(agency2Id, new CreateOrganizationUserRequest(
                "Agency 2 Manager", "fin_agu2_" + suffixB + "@print.com", "9876543012", "Password123!", "Manager"
        ));

        MvcResult ag2Login = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("fin_agu2_" + suffixB + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agency2Token = objectMapper.readTree(ag2Login.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var stu = studentService.createStudent(school1Id, new StudentRequest(
                "ADM-FIN-01", "Kavya Nair", "Father", "Mother",
                LocalDate.of(2011, 5, 12), "FEMALE", "B+", "10", "A", "01",
                "Address", "9876543013", null, null, "ACTIVE"
        ));

        String tplName = "Fin Sec Template " + suffix;
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
                        .content(objectMapper.writeValueAsString(new AssignTemplateRequest(school1Id, "2026-2027", true))))
                .andExpect(status().isOk());

        MvcResult orderRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                templateVerId, "2026-2027", "DIRECT_TO_SCHOOL",
                                "10 Fin Way", "Manager", "9876543010", null,
                                List.of(stu.id()), null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode orderData = objectMapper.readTree(orderRes.getResponse().getContentAsString()).path("data");
        orderId = UUID.fromString(orderData.path("id").asText());
        UUID orderItemId = UUID.fromString(orderData.path("items").get(0).path("id").asText());

        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token))
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
                                List.of(new QuotationItemRequest("Card Printing", 1, new BigDecimal("100.00")))
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/approve-quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token))
                .andExpect(status().isOk());

        var assignments = agencyJobService.assignOrderToAgencies(orderId, adminUserId, new AssignOrderRequest(
                List.of(new AgencyAssignmentItemRequest(
                        agency1Id,
                        LocalDate.now().plusDays(10),
                        new BigDecimal("45.00"),
                        "Assignment Note",
                        List.of(new JobItemAllocationRequest(orderItemId, 1))
                ))
        ));
        assignment1Id = assignments.get(0).id();

        mockMvc.perform(post("/api/v1/agency/jobs/" + assignment1Id + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agency1Token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Rule 1: School user is forbidden from accessing agency settlement endpoints")
    void schoolUserCannotAccessAgencySettlements() throws Exception {
        mockMvc.perform(get("/api/v1/agency/finance/settlements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rule 1: School invoices strictly hide all agency cost, assignment, and settlement data")
    void schoolInvoiceHidesAgencyData() throws Exception {
        // Admin generates invoice
        mockMvc.perform(post("/api/v1/admin/finance/orders/" + orderId + "/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateInvoiceRequest(
                                LocalDate.now().plusDays(30), "Tax invoice"
                        ))))
                .andExpect(status().isCreated());

        MvcResult res = mockMvc.perform(get("/api/v1/school/finance/invoices")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("data").get(0);
        assertThat(data.has("agencyId")).isFalse();
        assertThat(data.has("agencyCost")).isFalse();
        assertThat(data.has("agreedRate")).isFalse();
        assertThat(data.has("settlementNumber")).isFalse();
    }

    @Test
    @DisplayName("Rule 1: Agency settlement response strictly hides school selling price and quotation")
    void agencySettlementHidesSchoolPrice() throws Exception {
        // Admin generates settlement for Agency 1
        mockMvc.perform(post("/api/v1/admin/finance/assignments/" + assignment1Id + "/settlement")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateSettlementRequest(
                                BigDecimal.ZERO, BigDecimal.ZERO, "Full settlement"
                        ))))
                .andExpect(status().isCreated());

        MvcResult res = mockMvc.perform(get("/api/v1/agency/finance/settlements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agency1Token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("data").get(0);
        assertThat(data.has("schoolSellingPrice")).isFalse();
        assertThat(data.has("quotationAmount")).isFalse();
        assertThat(data.has("platformMargin")).isFalse();
        assertThat(data.has("schoolEmail")).isFalse();
    }

    @Test
    @DisplayName("Rule 2: School 2 cannot view or pay for School 1's order")
    void crossSchoolCannotPayOrder() throws Exception {
        RecordPaymentRequest request = new RecordPaymentRequest(
                new BigDecimal("50.00"), PaymentMethod.UPI, "UPI-REF-001", "Breach attempt"
        );

        mockMvc.perform(post("/api/v1/school/finance/orders/" + orderId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Rule 2: Agency 2 cannot view Agency 1's settlements")
    void crossAgencyCannotViewSettlements() throws Exception {
        // Generate settlement for Agency 1
        mockMvc.perform(post("/api/v1/admin/finance/assignments/" + assignment1Id + "/settlement")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateSettlementRequest(
                                BigDecimal.ZERO, BigDecimal.ZERO, "Agency 1 settlement"
                        ))))
                .andExpect(status().isCreated());

        // Agency 2 lists settlements: must return empty list (0 elements)
        MvcResult res = mockMvc.perform(get("/api/v1/agency/finance/settlements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agency2Token))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = objectMapper.readTree(res.getResponse().getContentAsString()).path("data").path("data");
        assertThat(data.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("Rule 2: Agency user cannot access Admin finance endpoints")
    void agencyUserCannotAccessAdminFinance() throws Exception {
        mockMvc.perform(get("/api/v1/admin/finance/summary")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agency1Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Rule 8: Validation failure on zero or negative payment amount")
    void invalidPaymentAmountValidation() throws Exception {
        RecordPaymentRequest request = new RecordPaymentRequest(
                new BigDecimal("0.00"), PaymentMethod.UPI, "UPI-ZERO", "Zero payment"
        );

        mockMvc.perform(post("/api/v1/school/finance/orders/" + orderId + "/payments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + school1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401")
    void unauthenticatedAccessReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/school/finance/summary"))
                .andExpect(status().isUnauthorized());
    }
}
