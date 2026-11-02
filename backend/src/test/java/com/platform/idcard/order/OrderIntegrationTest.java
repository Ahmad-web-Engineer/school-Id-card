package com.platform.idcard.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.order.dto.*;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.student.dto.StudentRequest;
import com.platform.idcard.student.service.StudentService;
import com.platform.idcard.template.dto.AssignTemplateRequest;
import com.platform.idcard.template.dto.CreateCardTemplateRequest;
import com.platform.idcard.template.dto.CreateTemplateVersionRequest;
import com.platform.idcard.template.service.CardTemplateService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderIntegrationTest {

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

    @Autowired
    private StudentService studentService;

    @Autowired
    private CardTemplateService cardTemplateService;

    private String adminToken;
    private UUID adminUserId;

    private String schoolAToken;
    private UUID schoolAId;
    private UUID studentA1Id;
    private UUID studentA2Id;

    private String schoolBToken;
    private UUID schoolBId;
    private UUID studentB1Id;

    private UUID approvedTemplateVersionId;

    @BeforeEach
    void setUp() throws Exception {
        // Ensure roles
        Role adminRole = roleRepository.findByRoleName("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "SUPER_ADMIN", "Platform Administrator")));
        if (roleRepository.findByRoleName("SCHOOL").isEmpty()) {
            roleRepository.save(new Role(null, "SCHOOL", "School Administrator"));
        }
        if (roleRepository.findByRoleName("PRINTING_AGENCY").isEmpty()) {
            roleRepository.save(new Role(null, "PRINTING_AGENCY", "Printing Agency Partner"));
        }

        // Ensure Admin user
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

        // Admin login
        MvcResult adminLoginRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@platform.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminLoginRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Setup School A
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        var schoolADto = schoolService.createSchool(new SchoolRequest(
                "Order Academy A " + suffixA, "ORD-A-" + suffixA, "10 Campus Road",
                "Metropolis", "State X", "500001", "order_school_a_" + suffixA + "@test.edu",
                "9876543001", "ACTIVE"
        ));
        schoolAId = schoolADto.id();

        schoolService.createSchoolUser(schoolAId, new CreateOrganizationUserRequest(
                "School A Manager", "manager_a_" + suffixA + "@test.edu", "9876543001", "Password123!", "Admin"
        ));

        MvcResult loginARes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("manager_a_" + suffixA + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolAToken = objectMapper.readTree(loginARes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Setup School B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        var schoolBDto = schoolService.createSchool(new SchoolRequest(
                "Order Academy B " + suffixB, "ORD-B-" + suffixB, "20 Campus Road",
                "Metropolis", "State X", "500001", "order_school_b_" + suffixB + "@test.edu",
                "9876543002", "ACTIVE"
        ));
        schoolBId = schoolBDto.id();

        schoolService.createSchoolUser(schoolBId, new CreateOrganizationUserRequest(
                "School B Manager", "manager_b_" + suffixB + "@test.edu", "9876543002", "Password123!", "Admin"
        ));

        MvcResult loginBRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("manager_b_" + suffixB + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolBToken = objectMapper.readTree(loginBRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Students in School A
        var stuA1 = studentService.createStudent(schoolAId, new StudentRequest(
                "ADM-A-001", "Aarav Sharma", "Rajesh Sharma", "Pooja Sharma",
                LocalDate.of(2011, 4, 10), "MALE", "O+", "10", "A", "01",
                "10 Rose Garden, Metropolis", "9876511111", null, null, "ACTIVE"
        ));
        studentA1Id = stuA1.id();

        var stuA2 = studentService.createStudent(schoolAId, new StudentRequest(
                "ADM-A-002", "Ananya Verma", "Suresh Verma", "Meena Verma",
                LocalDate.of(2011, 8, 22), "FEMALE", "B+", "10", "A", "02",
                "12 Rose Garden, Metropolis", "9876522222", null, null, "ACTIVE"
        ));
        studentA2Id = stuA2.id();

        // Student in School B
        var stuB1 = studentService.createStudent(schoolBId, new StudentRequest(
                "ADM-B-001", "Rohan Mehta", "Kamal Mehta", "Rekha Mehta",
                LocalDate.of(2012, 1, 15), "MALE", "A+", "9", "B", "05",
                "50 Oak Avenue, Metropolis", "9876533333", null, null, "ACTIVE"
        ));
        studentB1Id = stuB1.id();

        // Create and approve a Template via Admin MockMvc
        String tplName = "Global Order Template " + UUID.randomUUID().toString().substring(0, 8);
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

        // Submit & approve Version 1
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

        // Assign to School A
        mockMvc.perform(post("/api/v1/admin/templates/" + tplId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTemplateRequest(schoolAId, "2026-2027", true))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Create Draft Order with Student Snapshots and verify snapshot immutability")
    void testCreateDraftOrderAndSnapshotImmutability() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                approvedTemplateVersionId,
                "2026-2027",
                "DIRECT_TO_SCHOOL",
                "10 Campus Road, Metropolis",
                "Mr. Principal",
                "9876543001",
                "Handle cards carefully",
                List.of(studentA1Id, studentA2Id),
                List.of(new AdditionalItemRequest("LANYARD", 2))
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.orderNumber", startsWith("ORD-2026-")))
                .andExpect(jsonPath("$.data.orderStatus", is("DRAFT")))
                .andExpect(jsonPath("$.data.totalStudents", is(2)))
                .andExpect(jsonPath("$.data.totalQuantity", is(4))) // 2 cards + 2 lanyards
                .andExpect(jsonPath("$.data.items", hasSize(2)))
                .andExpect(jsonPath("$.data.students", hasSize(2)))
                .andReturn();

        JsonNode orderData = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        String orderId = orderData.path("id").asText();

        // Verify initial student snapshot has "Aarav Sharma"
        assertThat(orderData.path("students").get(0).path("studentName").asText()).isEqualTo("Aarav Sharma");

        // Now modify Student A1 in the school student roster (change name to "Aarav S. Patel")
        studentService.updateStudent(schoolAId, studentA1Id, new StudentRequest(
                "ADM-A-001", "Aarav S. Patel", "Rajesh Sharma", "Pooja Sharma",
                LocalDate.of(2011, 4, 10), "MALE", "O+", "10", "A", "01",
                "10 Rose Garden, Metropolis", "9876511111", null, null, "ACTIVE"
        ));

        // Retrieve order details again - verify snapshot still has original name "Aarav Sharma" (Immutability guarantee)
        mockMvc.perform(get("/api/v1/school/orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.students[0].studentName", is("Aarav Sharma")));
    }

    @Test
    @DisplayName("Cross-tenant Student Protection: School A cannot include School B's student in order")
    void testCrossTenantStudentOrderRejection() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                approvedTemplateVersionId,
                "2026-2027",
                "DIRECT_TO_SCHOOL",
                "10 Campus Road",
                "Coordinator",
                "9876543001",
                null,
                List.of(studentA1Id, studentB1Id), // studentB1Id belongs to School B!
                null
        );

        mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.message", containsString("does not belong to this school")));
    }

    @Test
    @DisplayName("Full Order Lifecycle: DRAFT -> SUBMITTED -> UNDER_REVIEW -> CORRECTION -> SUBMITTED -> VERIFIED -> QUOTED -> APPROVED")
    void testFullOrderLifecycleAndQuotationEngine() throws Exception {
        // 1. Create Draft Order
        CreateOrderRequest createReq = new CreateOrderRequest(
                approvedTemplateVersionId,
                "2026-2027",
                "DIRECT_TO_SCHOOL",
                "10 Campus Road",
                "Coordinator",
                "9876543001",
                "Urgent processing requested",
                List.of(studentA1Id),
                null
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();

        // 2. School submits order -> SUBMITTED
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("SUBMITTED")))
                .andExpect(jsonPath("$.data.submittedAt", notNullValue()));

        // 3. Admin starts review -> UNDER_REVIEW
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/start-review")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("UNDER_REVIEW")));

        // 4. Admin requests correction -> CORRECTION_REQUIRED
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/corrections")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RequestCorrectionRequest("Please update student emergency contact number"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("CORRECTION_REQUIRED")))
                .andExpect(jsonPath("$.data.rejectionRemarks", containsString("emergency contact")));

        // 5. School re-submits after addressing remarks -> SUBMITTED
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("SUBMITTED")));

        // 6. Admin verifies order -> VERIFIED
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("VERIFIED")))
                .andExpect(jsonPath("$.data.verifiedAt", notNullValue()))
                .andExpect(jsonPath("$.data.rejectionRemarks", nullValue()));

        // 7. Admin generates quotation (100 PVC cards @ 50.00, 100 Lanyards @ 15.00, Discount: 500.00, Tax: 18%)
        // Subtotal = 5000 + 1500 = 6500.00
        // Discount = 500.00
        // Taxable = 6000.00
        // Tax (18%) = 1080.00
        // Total = 7080.00
        GenerateQuotationRequest quoteReq = new GenerateQuotationRequest(
                new BigDecimal("500.00"),
                new BigDecimal("18.00"),
                15,
                "Net 15 days payment terms",
                List.of(
                        new QuotationItemRequest("PVC ID Card Printing", 100, new BigDecimal("50.00")),
                        new QuotationItemRequest("Standard 16mm Lanyard", 100, new BigDecimal("15.00"))
                )
        );

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(quoteReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.quotationNumber", startsWith("QUO-2026-")))
                .andExpect(jsonPath("$.data.subtotal", is(6500.0)))
                .andExpect(jsonPath("$.data.discount", is(500.0)))
                .andExpect(jsonPath("$.data.taxRate", is(18.0)))
                .andExpect(jsonPath("$.data.taxAmount", is(1080.0)))
                .andExpect(jsonPath("$.data.totalAmount", is(7080.0)))
                .andExpect(jsonPath("$.data.quotationStatus", is("GENERATED")));

        // 8. School views order - order status is now QUOTATION_GENERATED and quotation is visible
        mockMvc.perform(get("/api/v1/school/orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("QUOTATION_GENERATED")))
                .andExpect(jsonPath("$.data.quotation.totalAmount", is(7080.0)))
                .andExpect(jsonPath("$.data.quotation.items", hasSize(2)));

        // 9. School approves quotation -> status moves to APPROVED
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/approve-quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderStatus", is("APPROVED")))
                .andExpect(jsonPath("$.data.quotation.quotationStatus", is("APPROVED")));
    }

    @Test
    @DisplayName("Multi-tenant Role Isolation: School B cannot view or approve School A's order")
    void testMultiTenantOrderIsolation() throws Exception {
        // Create order for School A
        CreateOrderRequest request = new CreateOrderRequest(
                approvedTemplateVersionId,
                "2026-2027",
                "DIRECT_TO_SCHOOL",
                "10 Campus Road",
                "Coordinator",
                "9876543001",
                null,
                List.of(studentA1Id),
                null
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data").path("id").asText();

        // School B tries to access School A's order -> 404 Not Found (Rule 2)
        mockMvc.perform(get("/api/v1/school/orders/" + orderId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));

        // School B tries to submit School A's order -> 404 Not Found
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolBToken))
                .andExpect(status().isNotFound());

        // School user cannot access admin order endpoints -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isForbidden());
    }
}
