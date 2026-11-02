package com.platform.idcard.production;

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
import com.platform.idcard.order.dto.CreateOrderRequest;
import com.platform.idcard.order.dto.GenerateQuotationRequest;
import com.platform.idcard.order.dto.QuotationItemRequest;
import com.platform.idcard.order.entity.OrderStatus;
import com.platform.idcard.order.entity.PrintingOrder;
import com.platform.idcard.order.repository.PrintingOrderRepository;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.production.dto.ApproveReprintRequest;
import com.platform.idcard.production.dto.ProductionUpdateRequest;
import com.platform.idcard.production.dto.QualityCheckRequest;
import com.platform.idcard.production.entity.CostBearer;
import com.platform.idcard.production.entity.DefectCategory;
import com.platform.idcard.production.entity.QualityCheckStatus;
import com.platform.idcard.production.entity.ReprintStatus;
import com.platform.idcard.production.repository.ReprintRequestRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductionIntegrationTest {

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
    private PrintingOrderRepository printingOrderRepository;

    @Autowired
    private ReprintRequestRepository reprintRequestRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID adminUserId;
    private String schoolToken;
    private String agencyToken;

    private UUID schoolId;
    private UUID agencyId;
    private UUID orderId;
    private UUID jobId;

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
                "Prod Integ School " + suffix, "SCH-PI-" + suffix, "10 Campus Way",
                "Metropolis", "State X", "500001", "integ_prod_school_" + suffix + "@test.edu",
                "9876543010", "ACTIVE"
        ));
        schoolId = schoolDto.id();

        schoolService.createSchoolUser(schoolId, new CreateOrganizationUserRequest(
                "School Manager", "integ_prod_user_" + suffix + "@test.edu", "9876543010", "Password123!", "Admin"
        ));

        MvcResult schoolLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("integ_prod_user_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolToken = objectMapper.readTree(schoolLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var agDto = agencyService.createAgency(new AgencyRequest(
                "Print Corp " + suffix, "Director " + suffix, "printcorp_" + suffix + "@print.com",
                "9876512345", "Industrial Area", 10000, "500002", "ACTIVE"
        ));
        agencyId = agDto.id();

        agencyService.createAgencyUser(agencyId, new CreateOrganizationUserRequest(
                "Agency Manager", "operator_" + suffix + "@print.com", "9876512345", "Password123!", "Manager"
        ));

        MvcResult agLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("operator_" + suffix + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(agLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var stu = studentService.createStudent(schoolId, new StudentRequest(
                "ADM-PI-01", "Sneha " + suffix, "Father", "Mother",
                LocalDate.of(2011, 8, 20), "FEMALE", "A+", "8th", "B", "15",
                "Address", "9876543013", null, null, "ACTIVE"
        ));

        String tplName = "Prod Integ Template " + suffix;
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

        MvcResult orderRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                templateVerId, "2026-2027", "DIRECT_TO_SCHOOL",
                                "Gate 1", "Officer", "9876543010", null,
                                List.of(stu.id()), null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode orderData = objectMapper.readTree(orderRes.getResponse().getContentAsString()).path("data");
        orderId = UUID.fromString(orderData.path("id").asText());
        UUID orderItemId = UUID.fromString(orderData.path("items").get(0).path("id").asText());

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

        var assignments = agencyJobService.assignOrderToAgencies(orderId, adminUserId, new AssignOrderRequest(
                List.of(new AgencyAssignmentItemRequest(
                        agencyId,
                        LocalDate.now().plusDays(7),
                        new BigDecimal("18.00"),
                        "Note",
                        List.of(new JobItemAllocationRequest(orderItemId, 1))
                ))
        ));
        jobId = assignments.get(0).id();

        agencyJobService.acceptJob(jobId, agencyId, null);
    }

    private String loginAndGetToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Complete Step 7 Production & Quality Control Lifecycle: Start -> Print -> Defect QC -> Auto Reprint -> Admin Approve")
    void testCompleteProductionAndQCLifecycle() throws Exception {
        // Step 1: Agency starts production
        mockMvc.perform(post("/api/v1/agency/jobs/" + jobId + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assignmentStatus", is("IN_PRODUCTION")));

        PrintingOrder parentOrder = printingOrderRepository.findById(orderId).orElseThrow();
        assertThat(parentOrder.getOrderStatus()).isEqualTo(OrderStatus.IN_PRODUCTION.name());

        // Step 2: Agency lists batches (auto-created initial batch of 1 card)
        MvcResult batchesResult = mockMvc.perform(get("/api/v1/agency/jobs/" + jobId + "/batches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.data[0].batchNumber").exists())
                .andReturn();

        JsonNode batchesNode = objectMapper.readTree(batchesResult.getResponse().getContentAsString());
        UUID batchId = UUID.fromString(batchesNode.path("data").path("data").get(0).path("id").asText());

        // Step 3: Agency reports production progress update (1 card printed)
        mockMvc.perform(post("/api/v1/agency/batches/" + batchId + "/updates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductionUpdateRequest(
                                1, 0, "Card printed cleanly on thermal line", null
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.actualQuantity", is(1)))
                .andExpect(jsonPath("$.data.productionStatus", is("PRINTING_COMPLETED")));

        parentOrder = printingOrderRepository.findById(orderId).orElseThrow();
        assertThat(parentOrder.getOrderStatus()).isEqualTo(OrderStatus.PRINTING_COMPLETED.name());

        // Step 4: Quality Inspection detects printing defect (misaligned vertical edge)
        QualityCheckRequest qcRequest = new QualityCheckRequest(
                1, 0, 1, DefectCategory.PRINT_ALIGNMENT, "Vertical shift defect >2mm", "Rejected for reprint"
        );

        mockMvc.perform(post("/api/v1/agency/jobs/" + jobId + "/quality-checks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(qcRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.inspectionStatus", is(QualityCheckStatus.FAILED.name())))
                .andExpect(jsonPath("$.data.rejectedQuantity", is(1)));

        // Automated reprint request is generated in PENDING status
        var reprints = reprintRequestRepository.findAllByAssignmentId(jobId);
        assertThat(reprints).hasSize(1);
        assertThat(reprints.get(0).getApprovalStatus()).isEqualTo(ReprintStatus.PENDING);
        assertThat(reprints.get(0).getReplacementQuantity()).isEqualTo(1);

        parentOrder = printingOrderRepository.findById(orderId).orElseThrow();
        assertThat(parentOrder.getOrderStatus()).isEqualTo(OrderStatus.REPRINT_REQUIRED.name());

        // Step 5: Admin reviews and approves reprint request
        UUID reprintId = reprints.get(0).getId();
        mockMvc.perform(post("/api/v1/admin/reprints/" + reprintId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApproveReprintRequest(
                                CostBearer.AGENCY, "Approved. Defect due to agency thermal head drift."
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus", is(ReprintStatus.APPROVED.name())))
                .andExpect(jsonPath("$.data.costBearer", is(CostBearer.AGENCY.name())));

        // Statuses transition back to IN_PRODUCTION
        parentOrder = printingOrderRepository.findById(orderId).orElseThrow();
        assertThat(parentOrder.getOrderStatus()).isEqualTo(OrderStatus.IN_PRODUCTION.name());

        // Step 6: School accesses live order tracking endpoint (Zero agency leak)
        mockMvc.perform(get("/api/v1/school/orders/" + orderId + "/tracking")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.totalCards", is(1)))
                .andExpect(jsonPath("$.data.cardsPrinted", is(1)))
                .andExpect(jsonPath("$.data.percentComplete", is(100.0)))
                .andExpect(jsonPath("$.data.currentStage", is("PRINTING")));
    }
}
