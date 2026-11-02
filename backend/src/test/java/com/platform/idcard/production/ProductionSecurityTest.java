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
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.production.dto.CreateBatchRequest;
import com.platform.idcard.production.dto.QualityCheckRequest;
import com.platform.idcard.production.entity.DefectCategory;
import com.platform.idcard.production.service.ProductionService;
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
class ProductionSecurityTest {

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
    private ProductionService productionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID adminUserId;
    private String schoolToken;
    private String agencyAToken;
    private String agencyBToken;

    private UUID schoolId;
    private UUID agencyAId;
    private UUID agencyBId;
    private UUID orderId;
    private UUID jobAId;

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
                "Prod Sec School " + suffix, "SCH-PS-" + suffix, "10 Campus Way",
                "Metropolis", "State X", "500001", "sec_prod_school_" + suffix + "@test.edu",
                "9876543010", "ACTIVE"
        ));
        schoolId = schoolDto.id();

        schoolService.createSchoolUser(schoolId, new CreateOrganizationUserRequest(
                "School Manager", "sec_prod_user_" + suffix + "@test.edu", "9876543010", "Password123!", "Admin"
        ));

        MvcResult schoolLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("sec_prod_user_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolToken = objectMapper.readTree(schoolLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        var agADto = agencyService.createAgency(new AgencyRequest(
                "Print Agency A " + suffixA, "Contact A", "ag_pa_" + suffixA + "@print.com",
                "9876543011", "Industrial Area A", 50000, "500002", "ACTIVE"
        ));
        agencyAId = agADto.id();

        agencyService.createAgencyUser(agencyAId, new CreateOrganizationUserRequest(
                "Agency A Manager", "ag_pa_user_" + suffixA + "@print.com", "9876543011", "Password123!", "Manager"
        ));

        MvcResult agALogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ag_pa_user_" + suffixA + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyAToken = objectMapper.readTree(agALogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        var agBDto = agencyService.createAgency(new AgencyRequest(
                "Print Agency B " + suffixB, "Contact B", "ag_pb_" + suffixB + "@print.com",
                "9876543012", "Industrial Area B", 40000, "500003", "ACTIVE"
        ));
        agencyBId = agBDto.id();

        agencyService.createAgencyUser(agencyBId, new CreateOrganizationUserRequest(
                "Agency B Manager", "ag_pb_user_" + suffixB + "@print.com", "9876543012", "Password123!", "Manager"
        ));

        MvcResult agBLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("ag_pb_user_" + suffixB + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyBToken = objectMapper.readTree(agBLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        var stu = studentService.createStudent(schoolId, new StudentRequest(
                "ADM-PROD-01", "Rohan Gupta", "Father", "Mother",
                LocalDate.of(2011, 5, 12), "MALE", "O+", "10", "A", "01",
                "Address", "9876543013", null, null, "ACTIVE"
        ));

        String tplName = "Prod Sec Template " + suffix;
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
                                "10 Campus Way", "Manager", "9876543010", null,
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
                        agencyAId,
                        LocalDate.now().plusDays(10),
                        new BigDecimal("20.00"),
                        "Assignment Note",
                        List.of(new JobItemAllocationRequest(orderItemId, 1))
                ))
        ));
        jobAId = assignments.get(0).id();

        agencyJobService.acceptJob(jobAId, agencyAId, null);
    }

    @Test
    @DisplayName("Non-Negotiable Rule 1: School cannot access Agency production endpoints (403 Forbidden)")
    void testSchoolCannotAccessAgencyProduction() throws Exception {
        mockMvc.perform(post("/api/v1/agency/jobs/" + jobAId + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));

        mockMvc.perform(get("/api/v1/agency/jobs/" + jobAId + "/batches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-Negotiable Rule 1: School tracking response has zero agency data leak")
    void testSchoolTrackingLeakingNoAgencyData() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/school/orders/" + orderId + "/tracking")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").exists())
                .andExpect(jsonPath("$.data.cardsPrinted", is(0)))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).doesNotContain("Print Agency A");
        assertThat(responseBody).doesNotContain("JOB-2026");
        assertThat(responseBody).doesNotContain("20.00");
    }

    @Test
    @DisplayName("Non-Negotiable Rule 2: Agency B cannot start production or create batches for Agency A's job (404 Not Found)")
    void testAgencyCannotAccessOtherAgencyProduction() throws Exception {
        mockMvc.perform(post("/api/v1/agency/jobs/" + jobAId + "/start")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));

        mockMvc.perform(post("/api/v1/agency/jobs/" + jobAId + "/batches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBatchRequest(1, "Hacking batch"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Non-Negotiable Rule 2: Agency user cannot access Admin production inspection endpoints (403 Forbidden)")
    void testAgencyCannotAccessAdminProductionEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/production/batches")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));

        mockMvc.perform(get("/api/v1/admin/production/reprints")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-Negotiable Rule 8: Validation fails when inspectedQuantity != acceptedQuantity + rejectedQuantity")
    void testQualityCheckQuantitySumValidation() throws Exception {
        productionService.startProduction(jobAId, agencyAId, null);

        QualityCheckRequest invalidQc = new QualityCheckRequest(
                10, 5, 2, DefectCategory.PRINT_ALIGNMENT, "Defects", "Notes"
        );

        mockMvc.perform(post("/api/v1/agency/jobs/" + jobAId + "/quality-checks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidQc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Unauthenticated access to production endpoints is rejected (401 Unauthorized)")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/v1/agency/jobs/" + jobAId + "/batches"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));

        mockMvc.perform(get("/api/v1/admin/production/batches"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }
}
