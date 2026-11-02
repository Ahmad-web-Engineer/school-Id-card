package com.platform.idcard.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.order.dto.AdditionalItemRequest;
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
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security and Tenant Isolation Tests for Step 5: Orders and Quotations.
 *
 * Verifies Non-Negotiable Rules:
 * Rule 1: School never sees agency data.
 * Rule 2: Never trust tenant IDs from client payload; derive strictly from token.
 * Rule 3: Monetary precision and immutability.
 * Rule 4: Named business state transitions only.
 * Rule 6: Paginated responses and indexed queries.
 * Rule 7: Standard error envelopes.
 * Rule 8: Jakarta validation constraints.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderSecurityTest {

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
    private String schoolAToken;
    private UUID schoolAId;
    private UUID studentA1Id;
    private String schoolAOrderId;

    private String schoolBToken;
    private UUID schoolBId;
    private UUID studentB1Id;
    private String schoolBOrderId;

    private String agencyToken;

    private UUID approvedTemplateVersionId;

    @BeforeEach
    void setUp() throws Exception {
        // 1. Ensure Roles
        Role adminRole = roleRepository.findByRoleName("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "SUPER_ADMIN", "Admin")));
        Role schoolRole = roleRepository.findByRoleName("SCHOOL")
                .orElseGet(() -> roleRepository.save(new Role(null, "SCHOOL", "School")));
        Role agencyRole = roleRepository.findByRoleName("PRINTING_AGENCY")
                .orElseGet(() -> roleRepository.save(new Role(null, "PRINTING_AGENCY", "Agency")));

        // 2. Ensure Admin User & Login
        User adminUser = userRepository.findByEmail("admin@platform.com").orElse(null);
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setFullName("Super Admin");
            adminUser.setEmail("admin@platform.com");
            adminUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            adminUser.setStatus("ACTIVE");
            adminUser.setRoles(Set.of(adminRole));
            userRepository.save(adminUser);
        }

        MvcResult adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@platform.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // 3. Setup School A & User
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        var schoolADto = schoolService.createSchool(new SchoolRequest(
                "Sec School A " + suffixA, "SCH-SA-" + suffixA, "10 Campus A",
                "Metropolis", "State X", "500001", "sec_sch_a_" + suffixA + "@test.edu",
                "9876543001", "ACTIVE"
        ));
        schoolAId = schoolADto.id();

        schoolService.createSchoolUser(schoolAId, new CreateOrganizationUserRequest(
                "School A Manager", "sec_user_a_" + suffixA + "@test.edu", "9876543001", "Password123!", "Admin"
        ));

        MvcResult loginARes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("sec_user_a_" + suffixA + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolAToken = objectMapper.readTree(loginARes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // 4. Setup School B & User
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        var schoolBDto = schoolService.createSchool(new SchoolRequest(
                "Sec School B " + suffixB, "SCH-SB-" + suffixB, "20 Campus B",
                "Metropolis", "State X", "500001", "sec_sch_b_" + suffixB + "@test.edu",
                "9876543002", "ACTIVE"
        ));
        schoolBId = schoolBDto.id();

        schoolService.createSchoolUser(schoolBId, new CreateOrganizationUserRequest(
                "School B Manager", "sec_user_b_" + suffixB + "@test.edu", "9876543002", "Password123!", "Admin"
        ));

        MvcResult loginBRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("sec_user_b_" + suffixB + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolBToken = objectMapper.readTree(loginBRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // 5. Setup Printing Agency & User
        String suffixAg = UUID.randomUUID().toString().substring(0, 8);
        var agencyDto = agencyService.createAgency(new AgencyRequest(
                "Sec Agency " + suffixAg, "Contact Ag", "sec_ag_" + suffixAg + "@agency.com",
                "9876543003", "Ind Area", 50000, "500002", "ACTIVE"
        ));
        agencyService.createAgencyUser(agencyDto.id(), new CreateOrganizationUserRequest(
                "Agency Manager", "sec_ag_user_" + suffixAg + "@agency.com", "9876543003", "Password123!", "Manager"
        ));

        MvcResult loginAgRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("sec_ag_user_" + suffixAg + "@agency.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(loginAgRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // 6. Setup Students
        var stuA = studentService.createStudent(schoolAId, new StudentRequest(
                "ADM-SEC-A01", "Aarav Sharma", "Father", "Mother",
                LocalDate.of(2011, 4, 10), "MALE", "O+", "10", "A", "01",
                "Rose Garden", "9876511111", null, null, "ACTIVE"
        ));
        studentA1Id = stuA.id();

        var stuB = studentService.createStudent(schoolBId, new StudentRequest(
                "ADM-SEC-B01", "Rohan Mehta", "Father", "Mother",
                LocalDate.of(2012, 1, 15), "MALE", "A+", "9", "B", "05",
                "Oak Avenue", "9876533333", null, null, "ACTIVE"
        ));
        studentB1Id = stuB.id();

        // 7. Setup Template and Assign to School A & School B
        String tplName = "Sec Global Template " + UUID.randomUUID().toString().substring(0, 8);
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

        JsonNode tplData = objectMapper.readTree(tplRes.getResponse().getContentAsString()).path("data");
        String tplId = tplData.path("id").asText();
        String v1Id = tplData.path("versions").get(0).path("id").asText();

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
                        .content(objectMapper.writeValueAsString(new AssignTemplateRequest(schoolAId, "2026-2027", true))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/templates/" + tplId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTemplateRequest(schoolBId, "2026-2027", true))))
                .andExpect(status().isOk());

        // 8. Create Order for School A
        CreateOrderRequest reqA = new CreateOrderRequest(
                approvedTemplateVersionId, "2026-2027", "DIRECT_TO_SCHOOL",
                "10 Campus A", "Manager A", "9876543001", null,
                List.of(studentA1Id),
                List.of(new AdditionalItemRequest("LANYARD", 1))
        );
        MvcResult orderARes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqA)))
                .andExpect(status().isCreated())
                .andReturn();
        schoolAOrderId = objectMapper.readTree(orderARes.getResponse().getContentAsString()).path("data").path("id").asText();

        // 9. Create Order for School B
        CreateOrderRequest reqB = new CreateOrderRequest(
                approvedTemplateVersionId, "2026-2027", "DIRECT_TO_SCHOOL",
                "20 Campus B", "Manager B", "9876543002", null,
                List.of(studentB1Id),
                null
        );
        MvcResult orderBRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqB)))
                .andExpect(status().isCreated())
                .andReturn();
        schoolBOrderId = objectMapper.readTree(orderBRes.getResponse().getContentAsString()).path("data").path("id").asText();
    }

    @Test
    @DisplayName("Unauthenticated requests to orders endpoints must return 401 Unauthorized")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/school/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/orders"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/school/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Agency user cannot access School order endpoints (403 Forbidden)")
    void testAgencyCannotAccessSchoolOrders() throws Exception {
        mockMvc.perform(get("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));

        mockMvc.perform(get("/api/v1/school/orders/" + schoolAOrderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Agency user cannot access Admin order endpoints (403 Forbidden)")
    void testAgencyCannotAccessAdminOrders() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));

        mockMvc.perform(post("/api/v1/admin/orders/" + schoolAOrderId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("School user cannot access Admin order endpoints (403 Forbidden)")
    void testSchoolCannotAccessAdminOrders() throws Exception {
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));

        mockMvc.perform(post("/api/v1/admin/orders/" + schoolAOrderId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Rule 2: School A cannot view School B's order details (404 Not Found)")
    void testSchoolCannotViewOtherSchoolOrder() throws Exception {
        mockMvc.perform(get("/api/v1/school/orders/" + schoolBOrderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Rule 2: School A cannot submit School B's order (404 Not Found)")
    void testSchoolCannotSubmitOtherSchoolOrder() throws Exception {
        mockMvc.perform(post("/api/v1/school/orders/" + schoolBOrderId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Rule 2: School A cannot approve quotation for School B's order (404 Not Found)")
    void testSchoolCannotApproveOtherSchoolQuotation() throws Exception {
        mockMvc.perform(post("/api/v1/school/orders/" + schoolBOrderId + "/approve-quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Rule 2: School A cannot cancel School B's order (404 Not Found)")
    void testSchoolCannotCancelOtherSchoolOrder() throws Exception {
        mockMvc.perform(post("/api/v1/school/orders/" + schoolBOrderId + "/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Rule 2: School A cannot include School B's student in an order (404 Not Found)")
    void testCannotCreateOrderWithForeignStudent() throws Exception {
        CreateOrderRequest crossStudentReq = new CreateOrderRequest(
                approvedTemplateVersionId, "2026-2027", "DIRECT_TO_SCHOOL",
                "Campus Road", "Manager", "9876543100", null,
                List.of(studentB1Id), // Belongs to School B!
                null
        );

        mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(crossStudentReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("does not belong to this school")));
    }

    @Test
    @DisplayName("Rule 1: School Order Response must never expose agency data")
    void testSchoolResponseExcludesAgencyData() throws Exception {
        MvcResult res = mockMvc.perform(get("/api/v1/school/orders/" + schoolAOrderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andReturn();

        String content = res.getResponse().getContentAsString();
        assertThat(content).doesNotContain("agencyId");
        assertThat(content).doesNotContain("agencyName");
        assertThat(content).doesNotContain("purchasePrice");
        assertThat(content).doesNotContain("margin");
        assertThat(content).doesNotContain("internalJobNumber");
    }

    @Test
    @DisplayName("Rule 8: Jakarta validation blocks empty student list in order creation")
    void testOrderCreationValidationFailsOnEmptyStudents() throws Exception {
        CreateOrderRequest emptyStudentsReq = new CreateOrderRequest(
                approvedTemplateVersionId, "2026-2027", "DIRECT_TO_SCHOOL",
                "Campus Road", "Manager", "9876543100", null,
                List.of(), // Empty!
                null
        );

        mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyStudentsReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Rule 8: Jakarta validation blocks invalid quotation values")
    void testQuotationValidationFailsOnNegativeDiscount() throws Exception {
        // Submit order from School A
        mockMvc.perform(post("/api/v1/school/orders/" + schoolAOrderId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isOk());

        // Start review & verify as Admin
        mockMvc.perform(post("/api/v1/admin/orders/" + schoolAOrderId + "/start-review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/" + schoolAOrderId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        GenerateQuotationRequest invalidQuote = new GenerateQuotationRequest(
                new BigDecimal("-50.00"), // Negative discount!
                new BigDecimal("18.00"),
                15,
                "Notes",
                List.of(new QuotationItemRequest("Card Printing", 1, new BigDecimal("45.00")))
        );

        mockMvc.perform(post("/api/v1/admin/orders/" + schoolAOrderId + "/quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidQuote)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
    }
}
