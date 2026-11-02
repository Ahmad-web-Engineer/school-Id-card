package com.platform.idcard.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.security.JwtService;
import com.platform.idcard.auth.security.UserPrincipal;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.AgencyService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security and Tenant Isolation Tests for Step 4 (ID Card Template Designer).
 *
 * Verifies:
 * 1. School user cannot access admin template management endpoints (403 Forbidden / ACCESS_DENIED)
 * 2. Printing agency user cannot access admin or school template endpoints - Non-negotiable Rule 1 (403 Forbidden / ACCESS_DENIED)
 * 3. School tenant isolation:
 *    - School A cannot preview card using School B's studentId (404 Not Found / RESOURCE_NOT_FOUND)
 *    - School A cannot see templates assigned specifically to School B
 *    - School A cannot preview templates assigned specifically to School B (404 Not Found / RESOURCE_NOT_FOUND)
 * 4. Unauthenticated requests to /api/v1/admin/templates and /api/v1/school/templates return 401 Unauthorized
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardTemplateSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CardTemplateService cardTemplateService;

    @Autowired
    private RoleRepository roleRepository;

    private String adminToken;
    private UUID adminUserId;

    private String schoolAToken;
    private UUID schoolAId;
    private UUID studentAId;

    private String schoolBToken;
    private UUID schoolBId;
    private UUID studentBId;

    private String agencyToken;

    private UUID globalApprovedTemplateId;
    private UUID globalApprovedVersionId;

    private UUID schoolBTemplateId;

    private static final String BASIC_DESIGN_JSON = """
            {
              "elements": [
                {
                  "type": "text",
                  "field": "student_name",
                  "label": "Name:",
                  "x": 10,
                  "y": 15,
                  "fontSize": 12
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        // Ensure roles exist in DB
        if (roleRepository.findByRoleName("SUPER_ADMIN").isEmpty()) {
            roleRepository.save(new Role(null, "SUPER_ADMIN", "Administrator"));
        }
        if (roleRepository.findByRoleName("SCHOOL").isEmpty()) {
            roleRepository.save(new Role(null, "SCHOOL", "School role"));
        }
        if (roleRepository.findByRoleName("PRINTING_AGENCY").isEmpty()) {
            roleRepository.save(new Role(null, "PRINTING_AGENCY", "Agency role"));
        }

        // 1. Setup Super Admin
        adminUserId = UUID.randomUUID();
        UserPrincipal adminPrincipal = new UserPrincipal(
                adminUserId,
                "admin_sec@platform.com",
                "",
                "Security Admin",
                "SUPER_ADMIN",
                null,
                null,
                "Platform Admin",
                true
        );
        adminToken = jwtService.generateAccessToken(adminPrincipal);

        // 2. Setup School A & Student A
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        var schoolADto = schoolService.createSchool(new SchoolRequest(
                "School Alpha Security " + suffixA,
                "SCA-" + suffixA,
                "100 Alpha St",
                "City A",
                "State A",
                "100001",
                "school_a_" + suffixA + "@test.edu",
                "9811111111",
                "ACTIVE"
        ));
        schoolAId = schoolADto.id();

        UserPrincipal schoolAPrincipal = new UserPrincipal(
                UUID.randomUUID(),
                "principal_a_" + suffixA + "@test.edu",
                "",
                "Principal Alpha",
                "SCHOOL",
                schoolAId,
                null,
                "Principal",
                true
        );
        schoolAToken = jwtService.generateAccessToken(schoolAPrincipal);

        var studentADto = studentService.createStudent(schoolAId, new StudentRequest(
                "ADM-SEC-A1",
                "Student Alpha One",
                "Father Alpha",
                "Mother Alpha",
                LocalDate.of(2012, 1, 10),
                "MALE",
                "O+",
                "9",
                "A",
                "01",
                "Address Alpha",
                "9811111112",
                null,
                null,
                "ACTIVE"
        ));
        studentAId = studentADto.id();

        // 3. Setup School B & Student B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        var schoolBDto = schoolService.createSchool(new SchoolRequest(
                "School Beta Security " + suffixB,
                "SCB-" + suffixB,
                "200 Beta St",
                "City B",
                "State B",
                "200002",
                "school_b_" + suffixB + "@test.edu",
                "9822222222",
                "ACTIVE"
        ));
        schoolBId = schoolBDto.id();

        UserPrincipal schoolBPrincipal = new UserPrincipal(
                UUID.randomUUID(),
                "principal_b_" + suffixB + "@test.edu",
                "",
                "Principal Beta",
                "SCHOOL",
                schoolBId,
                null,
                "Principal",
                true
        );
        schoolBToken = jwtService.generateAccessToken(schoolBPrincipal);

        var studentBDto = studentService.createStudent(schoolBId, new StudentRequest(
                "ADM-SEC-B1",
                "Student Beta One",
                "Father Beta",
                "Mother Beta",
                LocalDate.of(2012, 2, 20),
                "FEMALE",
                "A+",
                "9",
                "B",
                "02",
                "Address Beta",
                "9822222223",
                null,
                null,
                "ACTIVE"
        ));
        studentBId = studentBDto.id();

        // 4. Setup Printing Agency User
        String suffixAgency = UUID.randomUUID().toString().substring(0, 8);
        var agencyDto = agencyService.createAgency(new AgencyRequest(
                "Agency Security " + suffixAgency,
                "Agency Contact",
                "agency_sec_" + suffixAgency + "@print.com",
                "9833333333",
                "Industrial Area B",
                10000,
                "Thermal PVC",
                "ACTIVE"
        ));

        UserPrincipal agencyPrincipal = new UserPrincipal(
                UUID.randomUUID(),
                "operator_" + suffixAgency + "@print.com",
                "",
                "Agency Operator",
                "PRINTING_AGENCY",
                null,
                agencyDto.id(),
                "Press Operator",
                true
        );
        agencyToken = jwtService.generateAccessToken(agencyPrincipal);

        // 5. Pre-create templates with admin security context
        try {
            var auth = new UsernamePasswordAuthenticationToken(adminPrincipal, null, adminPrincipal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Pre-create Global Approved Template (available to all schools)
            String globalTplName = "Global Security Template " + UUID.randomUUID().toString().substring(0, 8);
            var globalCreated = cardTemplateService.createTemplate(new CreateCardTemplateRequest(
                    globalTplName,
                    "STUDENT",
                    null, // Global
                    new BigDecimal("85.60"),
                    new BigDecimal("54.00"),
                    new BigDecimal("3.00"),
                    BASIC_DESIGN_JSON,
                    BASIC_DESIGN_JSON
            ));
            globalApprovedTemplateId = globalCreated.id();
            globalApprovedVersionId = globalCreated.versions().get(0).id();
            cardTemplateService.approveVersion(globalApprovedTemplateId, globalApprovedVersionId, adminUserId);

            // Pre-create School B Specific Approved Template (assigned specifically to School B, not global)
            String schoolBTplName = "School B Dedicated Template " + UUID.randomUUID().toString().substring(0, 8);
            var schoolBCreated = cardTemplateService.createTemplate(new CreateCardTemplateRequest(
                    schoolBTplName,
                    "STUDENT",
                    schoolBId, // Assigned specifically to School B
                    new BigDecimal("85.60"),
                    new BigDecimal("54.00"),
                    new BigDecimal("3.00"),
                    BASIC_DESIGN_JSON,
                    BASIC_DESIGN_JSON
            ));
            schoolBTemplateId = schoolBCreated.id();
            UUID schoolBV1Id = schoolBCreated.versions().get(0).id();
            cardTemplateService.approveVersion(schoolBTemplateId, schoolBV1Id, adminUserId);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    // =========================================================================
    // 1. School user cannot access admin template management endpoints
    // =========================================================================

    @Test
    @DisplayName("Security Requirement 1: School user cannot list admin templates (GET /api/v1/admin/templates -> 403 Forbidden)")
    void schoolUserCannotListAdminTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Security Requirement 1: School user cannot create card template (POST /api/v1/admin/templates -> 403 Forbidden)")
    void schoolUserCannotCreateTemplate() throws Exception {
        CreateCardTemplateRequest request = new CreateCardTemplateRequest(
                "Unauthorized School Template " + UUID.randomUUID().toString().substring(0, 8),
                "STUDENT",
                schoolAId,
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                BASIC_DESIGN_JSON,
                BASIC_DESIGN_JSON
        );

        mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Security Requirement 1: School user cannot create template version (POST /api/v1/admin/templates/{id}/versions -> 403 Forbidden)")
    void schoolUserCannotCreateTemplateVersion() throws Exception {
        CreateTemplateVersionRequest request = new CreateTemplateVersionRequest(
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                BASIC_DESIGN_JSON,
                BASIC_DESIGN_JSON
        );

        mockMvc.perform(post("/api/v1/admin/templates/" + globalApprovedTemplateId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Security Requirement 1: School user cannot assign template to school (POST /api/v1/admin/templates/{id}/assign -> 403 Forbidden)")
    void schoolUserCannotAssignTemplate() throws Exception {
        AssignTemplateRequest request = new AssignTemplateRequest(
                schoolAId,
                "2026-2027",
                true
        );

        mockMvc.perform(post("/api/v1/admin/templates/" + globalApprovedTemplateId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Security Requirement 1: School user cannot get template details with version history (GET /api/v1/admin/templates/{id} -> 403 Forbidden)")
    void schoolUserCannotGetAdminTemplateDetails() throws Exception {
        mockMvc.perform(get("/api/v1/admin/templates/" + globalApprovedTemplateId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Security Requirement 1: School user cannot submit version for approval (POST /api/v1/admin/templates/{id}/versions/{vId}/submit -> 403 Forbidden)")
    void schoolUserCannotSubmitVersion() throws Exception {
        mockMvc.perform(post("/api/v1/admin/templates/" + globalApprovedTemplateId + "/versions/" + globalApprovedVersionId + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Security Requirement 1: School user cannot approve template version (POST /api/v1/admin/templates/{id}/versions/{vId}/approve -> 403 Forbidden)")
    void schoolUserCannotApproveVersion() throws Exception {
        mockMvc.perform(post("/api/v1/admin/templates/" + globalApprovedTemplateId + "/versions/" + globalApprovedVersionId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Security Requirement 1: School user cannot access admin preview endpoint (GET /api/v1/admin/templates/{id}/versions/{vId}/preview -> 403 Forbidden)")
    void schoolUserCannotAccessAdminPreview() throws Exception {
        mockMvc.perform(get("/api/v1/admin/templates/" + globalApprovedTemplateId + "/versions/" + globalApprovedVersionId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    // =========================================================================
    // 2. Printing agency user cannot access admin or school template endpoints (Rule 1)
    // =========================================================================

    @Test
    @DisplayName("Non-negotiable Rule 1: Agency user cannot list admin templates (GET /api/v1/admin/templates -> 403 Forbidden)")
    void agencyUserCannotListAdminTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Agency user cannot access school templates catalog (GET /api/v1/school/templates -> 403 Forbidden)")
    void agencyUserCannotListSchoolTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/school/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Agency user cannot preview card on school endpoint (GET /api/v1/school/templates/{id}/preview -> 403 Forbidden)")
    void agencyUserCannotPreviewSchoolTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/school/templates/" + globalApprovedTemplateId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Agency user cannot create card templates (POST /api/v1/admin/templates -> 403 Forbidden)")
    void agencyUserCannotCreateTemplate() throws Exception {
        CreateCardTemplateRequest request = new CreateCardTemplateRequest(
                "Agency Illicit Template " + UUID.randomUUID().toString().substring(0, 8),
                "STUDENT",
                null,
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                BASIC_DESIGN_JSON,
                BASIC_DESIGN_JSON
        );

        mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    // =========================================================================
    // 3. School tenant isolation
    // =========================================================================

    @Test
    @DisplayName("Tenant Isolation: School A cannot preview card using School B's studentId (GET /api/v1/school/templates/{id}/preview?studentId={schoolBStudentId} -> 404 Not Found)")
    void schoolACannotPreviewCardWithSchoolBStudent() throws Exception {
        // School A attempts to preview template using Student B's ID
        mockMvc.perform(get("/api/v1/school/templates/" + globalApprovedTemplateId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .param("studentId", studentBId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Student not found in this school")));
    }

    @Test
    @DisplayName("Tenant Isolation: School A cannot see templates assigned specifically to School B (catalog isolation)")
    void schoolACannotSeeTemplatesAssignedSpecificallyToSchoolB() throws Exception {
        // School B lists templates -> sees its dedicated template
        mockMvc.perform(get("/api/v1/school/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[?(@.id == '" + schoolBTemplateId + "')]").exists())
                .andExpect(jsonPath("$.data[?(@.id == '" + globalApprovedTemplateId + "')]").exists());

        // School A lists templates -> sees global template, but CANNOT see School B's dedicated template
        mockMvc.perform(get("/api/v1/school/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data[?(@.id == '" + globalApprovedTemplateId + "')]").exists())
                .andExpect(jsonPath("$.data[?(@.id == '" + schoolBTemplateId + "')]").doesNotExist());
    }

    @Test
    @DisplayName("Tenant Isolation: School A cannot preview template assigned specifically to School B (GET /api/v1/school/templates/{schoolBTemplateId}/preview -> 404 Not Found)")
    void schoolACannotPreviewTemplateAssignedToSchoolB() throws Exception {
        // School A attempts to preview School B's dedicated template
        mockMvc.perform(get("/api/v1/school/templates/" + schoolBTemplateId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Template is not assigned to this school")));
    }

    @Test
    @DisplayName("Legitimate Access: School A can successfully preview card with its own enrolled student")
    void schoolACanPreviewOwnStudentWithAccessibleTemplate() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/school/templates/" + globalApprovedTemplateId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolAToken)
                        .param("studentId", studentAId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(200);
        String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length), StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Legitimate Access: School B can successfully preview its dedicated template with its own enrolled student")
    void schoolBCanPreviewDedicatedTemplateWithOwnStudent() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/school/templates/" + schoolBTemplateId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolBToken)
                        .param("studentId", studentBId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        byte[] pdfBytes = result.getResponse().getContentAsByteArray();
        assertThat(pdfBytes).isNotNull();
        assertThat(pdfBytes.length).isGreaterThan(200);
        String header = new String(pdfBytes, 0, Math.min(5, pdfBytes.length), StandardCharsets.US_ASCII);
        assertThat(header).isEqualTo("%PDF-");
    }

    // =========================================================================
    // 4. Unauthenticated requests return 401 Unauthorized
    // =========================================================================

    @Test
    @DisplayName("Security Requirement 4: Unauthenticated GET /api/v1/admin/templates returns 401 Unauthorized")
    void unauthenticatedCannotListAdminTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/admin/templates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security Requirement 4: Unauthenticated POST /api/v1/admin/templates returns 401 Unauthorized")
    void unauthenticatedCannotCreateAdminTemplate() throws Exception {
        CreateCardTemplateRequest request = new CreateCardTemplateRequest(
                "Unauth Template " + UUID.randomUUID().toString().substring(0, 8),
                "STUDENT",
                null,
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                BASIC_DESIGN_JSON,
                BASIC_DESIGN_JSON
        );

        mockMvc.perform(post("/api/v1/admin/templates")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security Requirement 4: Unauthenticated POST /api/v1/admin/templates/{id}/versions returns 401 Unauthorized")
    void unauthenticatedCannotCreateTemplateVersion() throws Exception {
        CreateTemplateVersionRequest request = new CreateTemplateVersionRequest(
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                BASIC_DESIGN_JSON,
                BASIC_DESIGN_JSON
        );

        mockMvc.perform(post("/api/v1/admin/templates/" + globalApprovedTemplateId + "/versions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security Requirement 4: Unauthenticated POST /api/v1/admin/templates/{id}/assign returns 401 Unauthorized")
    void unauthenticatedCannotAssignTemplate() throws Exception {
        AssignTemplateRequest request = new AssignTemplateRequest(
                schoolAId,
                "2026-2027",
                true
        );

        mockMvc.perform(post("/api/v1/admin/templates/" + globalApprovedTemplateId + "/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security Requirement 4: Unauthenticated GET /api/v1/school/templates returns 401 Unauthorized")
    void unauthenticatedCannotListSchoolTemplates() throws Exception {
        mockMvc.perform(get("/api/v1/school/templates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security Requirement 4: Unauthenticated GET /api/v1/school/templates/{id}/preview returns 401 Unauthorized")
    void unauthenticatedCannotPreviewSchoolTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/school/templates/" + globalApprovedTemplateId + "/preview"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }
}
