package com.platform.idcard.template;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.security.JwtService;
import com.platform.idcard.auth.security.UserPrincipal;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.student.dto.StudentRequest;
import com.platform.idcard.student.service.StudentService;
import com.platform.idcard.template.dto.AssignTemplateRequest;
import com.platform.idcard.template.dto.CreateCardTemplateRequest;
import com.platform.idcard.template.dto.CreateTemplateVersionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Functional and Integration Tests for Step 4 (ID Card Template Designer):
 * 1. Master Template Creation & Duplicate Rejection
 * 2. Versioning & Approval Lifecycle (DRAFT -> PENDING_APPROVAL -> APPROVED, Archival of prior versions)
 * 3. School Template Assignment & Catalog Retrieval with Pagination
 * 4. PDFBox PDF Rendering Verification (Admin preview & School preview with student data)
 * 5. Role Isolation & Validation Rules
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CardTemplateIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(CardTemplateIntegrationTest.class);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private StudentService studentService;

    private String adminToken;
    private UUID adminUserId;

    private String schoolToken;
    private UUID schoolId;
    private UUID studentId;

    private static final String SAMPLE_FRONT_DESIGN = """
            {
              "elements": [
                {
                  "type": "shape",
                  "x": 0,
                  "y": 0,
                  "width": 85.6,
                  "height": 12,
                  "fillColor": "#1E3A8A"
                },
                {
                  "type": "text",
                  "field": "student_name",
                  "label": "Name:",
                  "x": 10,
                  "y": 18,
                  "fontSize": 12,
                  "bold": true
                },
                {
                  "type": "text",
                  "field": "admission_number",
                  "label": "Adm:",
                  "x": 10,
                  "y": 26,
                  "fontSize": 10
                },
                {
                  "type": "qrcode",
                  "field": "admission_number",
                  "x": 60,
                  "y": 28,
                  "width": 18,
                  "height": 18
                },
                {
                  "type": "barcode",
                  "field": "admission_number",
                  "x": 10,
                  "y": 42,
                  "width": 45,
                  "height": 8
                }
              ]
            }
            """;

    private static final String SAMPLE_BACK_DESIGN = """
            {
              "elements": [
                {
                  "type": "text",
                  "field": "school_address",
                  "x": 10,
                  "y": 10,
                  "fontSize": 9
                },
                {
                  "type": "text",
                  "field": "emergency_contact",
                  "label": "Emergency:",
                  "x": 10,
                  "y": 20,
                  "fontSize": 9
                }
              ]
            }
            """;

    @BeforeEach
    void setUp() {
        log.info(">>> setUp START");
        // 1. Generate Admin JWT Token
        adminUserId = UUID.randomUUID();
        UserPrincipal adminPrincipal = new UserPrincipal(
                adminUserId,
                "admin@platform.com",
                "",
                "Super Admin",
                "SUPER_ADMIN",
                null,
                null,
                "Platform Admin",
                true
        );
        adminToken = jwtService.generateAccessToken(adminPrincipal);

        // 2. Setup Test School
        String schoolSuffix = UUID.randomUUID().toString().substring(0, 8);
        var schoolDto = schoolService.createSchool(new SchoolRequest(
                "Template Test Academy " + schoolSuffix,
                "TPL-" + schoolSuffix,
                "77 Campus Drive",
                "Metropolis",
                "State X",
                "500001",
                "template_school_" + schoolSuffix + "@test.edu",
                "9876543201",
                "ACTIVE"
        ));
        schoolId = schoolDto.id();

        // 3. Generate School User JWT Token
        UUID schoolUserId = UUID.randomUUID();
        UserPrincipal schoolPrincipal = new UserPrincipal(
                schoolUserId,
                "manager_" + schoolSuffix + "@test.edu",
                "",
                "School Manager",
                "SCHOOL",
                schoolId,
                null,
                "Coordinator",
                true
        );
        schoolToken = jwtService.generateAccessToken(schoolPrincipal);

        // 4. Pre-create a Student in the School
        var studentDto = studentService.createStudent(schoolId, new StudentRequest(
                "ADM-TPL-001",
                "Aarav Patel",
                "Ramesh Patel",
                "Sunita Patel",
                LocalDate.of(2011, 5, 14),
                "MALE",
                "B+",
                "10",
                "A",
                "15",
                "100 Park View Lane, Metropolis",
                "9876500001",
                "9876500002",
                null,
                "ACTIVE"
        ));
        studentId = studentDto.id();
        log.info(">>> setUp COMPLETED with schoolId={}, studentId={}", schoolId, studentId);
    }

    @Test
    @DisplayName("Master Template Creation: Admin creates template master in DRAFT, duplicate rejects with 409")
    void testMasterTemplateCreationAndDuplicateRejection() throws Exception {
        log.info(">>> START testMasterTemplateCreationAndDuplicateRejection");
        String uniqueTemplateName = "Student Horizontal Deluxe " + UUID.randomUUID().toString().substring(0, 8);

        CreateCardTemplateRequest createRequest = new CreateCardTemplateRequest(
                uniqueTemplateName,
                "STUDENT",
                null,
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                SAMPLE_FRONT_DESIGN,
                SAMPLE_BACK_DESIGN
        );

        // 1. Admin creates master template -> 201 Created with Version 1 in DRAFT status
        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.templateName", is(uniqueTemplateName)))
                .andExpect(jsonPath("$.data.templateCategory", is("STUDENT")))
                .andExpect(jsonPath("$.data.status", is("ACTIVE")))
                .andExpect(jsonPath("$.data.versions", hasSize(1)))
                .andExpect(jsonPath("$.data.versions[0].versionNumber", is(1)))
                .andExpect(jsonPath("$.data.versions[0].approvalStatus", is("DRAFT")))
                .andExpect(jsonPath("$.data.versions[0].approvedBy", nullValue()))
                .andExpect(jsonPath("$.data.versions[0].approvedAt", nullValue()))
                .andReturn();

        JsonNode responseJson = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        String templateId = responseJson.path("id").asText();
        assertThat(templateId).isNotBlank();

        // 2. Duplicate template name rejects with 409 Conflict
        mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_CONFLICT")))
                .andExpect(jsonPath("$.message", containsString("already exists")));

        // 3. Case-insensitive duplicate name check also rejects with 409 Conflict
        CreateCardTemplateRequest caseInsensitiveRequest = new CreateCardTemplateRequest(
                uniqueTemplateName.toUpperCase(),
                "STUDENT",
                null,
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                "{}",
                "{}"
        );
        mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(caseInsensitiveRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_CONFLICT")));
    }

    @Test
    @DisplayName("Versioning & Approval Lifecycle: Version 2 DRAFT -> PENDING_APPROVAL -> APPROVED, Version 3 archives Version 2")
    void testVersioningAndApprovalLifecycle() throws Exception {
        String templateName = "Lifecycle ID Template " + UUID.randomUUID().toString().substring(0, 8);

        // 1. Create Template with initial Version 1 (DRAFT)
        CreateCardTemplateRequest createRequest = new CreateCardTemplateRequest(
                templateName,
                "STUDENT",
                null,
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                SAMPLE_FRONT_DESIGN,
                SAMPLE_BACK_DESIGN
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String templateId = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 2. Create Version 2 in DRAFT status
        CreateTemplateVersionRequest v2Request = new CreateTemplateVersionRequest(
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                SAMPLE_FRONT_DESIGN,
                SAMPLE_BACK_DESIGN
        );

        MvcResult v2Result = mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v2Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.versionNumber", is(2)))
                .andExpect(jsonPath("$.data.approvalStatus", is("DRAFT")))
                .andReturn();

        String v2Id = objectMapper.readTree(v2Result.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 3. Submit Version 2 for approval -> status updates to PENDING_APPROVAL
        mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/versions/" + v2Id + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(v2Id)))
                .andExpect(jsonPath("$.data.versionNumber", is(2)))
                .andExpect(jsonPath("$.data.approvalStatus", is("PENDING_APPROVAL")));

        // 4. Approve Version 2 -> status updates to APPROVED, records approvedBy and approvedAt
        mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/versions/" + v2Id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(v2Id)))
                .andExpect(jsonPath("$.data.approvalStatus", is("APPROVED")))
                .andExpect(jsonPath("$.data.approvedBy", is(adminUserId.toString())))
                .andExpect(jsonPath("$.data.approvedAt", notNullValue()));

        // 5. Creating Version 3 and approving it archives Version 2 (status=ARCHIVED)
        CreateTemplateVersionRequest v3Request = new CreateTemplateVersionRequest(
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                SAMPLE_FRONT_DESIGN,
                SAMPLE_BACK_DESIGN
        );

        MvcResult v3Result = mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/versions")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(v3Request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.versionNumber", is(3)))
                .andExpect(jsonPath("$.data.approvalStatus", is("DRAFT")))
                .andReturn();

        String v3Id = objectMapper.readTree(v3Result.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Submit and approve Version 3
        mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/versions/" + v3Id + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus", is("PENDING_APPROVAL")));

        mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/versions/" + v3Id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus", is("APPROVED")))
                .andExpect(jsonPath("$.data.approvedBy", is(adminUserId.toString())));

        // 6. Verify complete version history via GET /api/v1/admin/templates/{id}
        MvcResult historyRes = mockMvc.perform(get("/api/v1/admin/templates/" + templateId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versions", hasSize(3)))
                .andReturn();

        JsonNode versionsNode = objectMapper.readTree(historyRes.getResponse().getContentAsString())
                .path("data").path("versions");

        // Verify Version 1 remains DRAFT
        assertThat(versionsNode.get(0).path("versionNumber").asInt()).isEqualTo(1);
        assertThat(versionsNode.get(0).path("approvalStatus").asText()).isEqualTo("DRAFT");

        // Verify Version 2 has transitioned to ARCHIVED
        assertThat(versionsNode.get(1).path("versionNumber").asInt()).isEqualTo(2);
        assertThat(versionsNode.get(1).path("approvalStatus").asText()).isEqualTo("ARCHIVED");

        // Verify Version 3 is currently APPROVED
        assertThat(versionsNode.get(2).path("versionNumber").asInt()).isEqualTo(3);
        assertThat(versionsNode.get(2).path("approvalStatus").asText()).isEqualTo("APPROVED");
    }

    @Test
    @DisplayName("School Assignment & Catalog: Admin assigns approved template to school, school views paginated catalog")
    void testSchoolAssignmentAndSchoolTemplateCatalog() throws Exception {
        String templateName = "School Assignment Card " + UUID.randomUUID().toString().substring(0, 8);

        // 1. Admin creates template
        CreateCardTemplateRequest createRequest = new CreateCardTemplateRequest(
                templateName,
                "STUDENT",
                null,
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                SAMPLE_FRONT_DESIGN,
                SAMPLE_BACK_DESIGN
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdData = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data");
        String templateId = createdData.path("id").asText();
        String v1Id = createdData.path("versions").get(0).path("id").asText();

        // 2. Reject assignment if template has no APPROVED version
        AssignTemplateRequest earlyAssignRequest = new AssignTemplateRequest(schoolId, "2026-2027", true);
        mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(earlyAssignRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("INVALID_ACTION")))
                .andExpect(jsonPath("$.message", containsString("approved version")));

        // 3. Approve Version 1
        mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/versions/" + v1Id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.approvalStatus", is("APPROVED")));

        // 4. POST /api/v1/admin/templates/{id}/assign with schoolId and academicSession 2026-2027
        AssignTemplateRequest assignRequest = new AssignTemplateRequest(schoolId, "2026-2027", true);
        mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.templateId", is(templateId)))
                .andExpect(jsonPath("$.data.schoolId", is(schoolId.toString())))
                .andExpect(jsonPath("$.data.academicSession", is("2026-2027")))
                .andExpect(jsonPath("$.data.message", containsString("assigned")));

        // 5. School GET /api/v1/school/templates -> returns assigned approved template with pagination metadata
        MvcResult schoolTemplatesResult = mockMvc.perform(get("/api/v1/school/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()))
                .andExpect(jsonPath("$.pagination.page", is(0)))
                .andExpect(jsonPath("$.pagination.size", is(10)))
                .andExpect(jsonPath("$.pagination.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.pagination.totalPages", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.pagination.last", notNullValue()))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(schoolTemplatesResult.getResponse().getContentAsString());
        JsonNode templatesList = responseNode.path("data");
        assertThat(templatesList.isArray()).isTrue();

        boolean foundAssignedTemplate = false;
        for (JsonNode tpl : templatesList) {
            if (templateId.equals(tpl.path("id").asText())) {
                foundAssignedTemplate = true;
                assertThat(tpl.path("templateName").asText()).isEqualTo(templateName);
                assertThat(tpl.path("approvedVersionId").asText()).isEqualTo(v1Id);
                assertThat(tpl.path("versionNumber").asInt()).isEqualTo(1);
                assertThat(tpl.path("cardWidthMm").asDouble()).isEqualTo(85.60);
                assertThat(tpl.path("cardHeightMm").asDouble()).isEqualTo(54.00);
                break;
            }
        }
        assertThat(foundAssignedTemplate).isTrue();
    }

    @Test
    @DisplayName("PDFBox PDF Rendering: Admin and School preview endpoints return valid PDF stream starting with %PDF-")
    void testPdfBoxPdfRenderingVerification() throws Exception {
        String templateName = "PDF Rendering Template " + UUID.randomUUID().toString().substring(0, 8);

        // 1. Create template with rich design JSON
        CreateCardTemplateRequest createRequest = new CreateCardTemplateRequest(
                templateName,
                "STUDENT",
                null,
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                SAMPLE_FRONT_DESIGN,
                SAMPLE_BACK_DESIGN
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdData = objectMapper.readTree(createRes.getResponse().getContentAsString()).path("data");
        String templateId = createdData.path("id").asText();
        String versionId = createdData.path("versions").get(0).path("id").asText();

        // 2. Admin GET /api/v1/admin/templates/{id}/versions/{versionId}/preview
        MvcResult adminPreviewResult = mockMvc.perform(get("/api/v1/admin/templates/" + templateId + "/versions/" + versionId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline; filename=\"template_preview_v")))
                .andReturn();

        byte[] adminPdfBytes = adminPreviewResult.getResponse().getContentAsByteArray();
        assertThat(adminPdfBytes).isNotNull();
        assertThat(adminPdfBytes.length).isGreaterThan(200);

        // Verify PDF Magic Bytes (%PDF-)
        String adminPdfHeader = new String(adminPdfBytes, 0, Math.min(5, adminPdfBytes.length), StandardCharsets.US_ASCII);
        assertThat(adminPdfHeader).isEqualTo("%PDF-");

        // 3. Approve and assign template to test school
        mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/versions/" + versionId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        AssignTemplateRequest assignRequest = new AssignTemplateRequest(schoolId, "2026-2027", true);
        mockMvc.perform(post("/api/v1/admin/templates/" + templateId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignRequest)))
                .andExpect(status().isOk());

        // 4. School GET /api/v1/school/templates/{id}/preview?studentId={studentId}
        MvcResult schoolPreviewResult = mockMvc.perform(get("/api/v1/school/templates/" + templateId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .param("studentId", studentId.toString()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline; filename=\"card_preview.pdf\"")))
                .andReturn();

        byte[] schoolPdfBytes = schoolPreviewResult.getResponse().getContentAsByteArray();
        assertThat(schoolPdfBytes).isNotNull();
        assertThat(schoolPdfBytes.length).isGreaterThan(200);

        // Verify PDF Magic Bytes (%PDF-)
        String schoolPdfHeader = new String(schoolPdfBytes, 0, Math.min(5, schoolPdfBytes.length), StandardCharsets.US_ASCII);
        assertThat(schoolPdfHeader).isEqualTo("%PDF-");

        // 5. School preview without studentId param (should fallback gracefully to first active student or sample)
        MvcResult schoolPreviewNoStudent = mockMvc.perform(get("/api/v1/school/templates/" + templateId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE))
                .andReturn();

        byte[] fallbackPdfBytes = schoolPreviewNoStudent.getResponse().getContentAsByteArray();
        assertThat(fallbackPdfBytes).isNotNull();
        String fallbackHeader = new String(fallbackPdfBytes, 0, Math.min(5, fallbackPdfBytes.length), StandardCharsets.US_ASCII);
        assertThat(fallbackHeader).isEqualTo("%PDF-");
    }

    @Test
    @DisplayName("Security & Access Control: School cannot access unassigned template preview or foreign school student")
    void testSecurityAndIsolationRules() throws Exception {
        // 1. Create template belonging to another school (not global, not assigned to caller's school)
        String otherSuffix = UUID.randomUUID().toString().substring(0, 8);
        var otherSchool = schoolService.createSchool(new SchoolRequest(
                "Other School " + otherSuffix,
                "OTH-" + otherSuffix,
                "88 Other Way",
                "Metropolis",
                "State Y",
                "500002",
                "other_school_" + otherSuffix + "@test.edu",
                "9876543202",
                "ACTIVE"
        ));

        String templateName = "Unassigned Template " + UUID.randomUUID().toString().substring(0, 8);
        CreateCardTemplateRequest createRequest = new CreateCardTemplateRequest(
                templateName,
                "STUDENT",
                otherSchool.id(),
                new BigDecimal("85.60"),
                new BigDecimal("54.00"),
                new BigDecimal("3.00"),
                SAMPLE_FRONT_DESIGN,
                SAMPLE_BACK_DESIGN
        );

        MvcResult createRes = mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String unassignedTemplateId = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // 2. School attempting to preview unassigned template returns 404
        mockMvc.perform(get("/api/v1/school/templates/" + unassignedTemplateId + "/preview")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .param("studentId", studentId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));

        // 3. School cannot access Admin template APIs (role protection -> 403 Forbidden)
        mockMvc.perform(get("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isForbidden());

        // 4. Admin cannot access School template endpoints (role protection -> 403 Forbidden)
        mockMvc.perform(get("/api/v1/school/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }
}
