package com.platform.idcard.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.organization.repository.SchoolRepository;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.student.dto.StudentRequest;
import com.platform.idcard.student.entity.PhotoImportBatch;
import com.platform.idcard.student.entity.StudentImportBatch;
import com.platform.idcard.student.repository.PhotoImportBatchRepository;
import com.platform.idcard.student.repository.StudentImportBatchRepository;
import com.platform.idcard.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentImportSecurityTest {

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
    private SchoolRepository schoolRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentImportBatchRepository studentImportBatchRepository;

    @Autowired
    private PhotoImportBatchRepository photoImportBatchRepository;

    private String schoolAToken;
    private UUID schoolAId;

    private String schoolBToken;
    private UUID schoolBId;
    private UUID studentBId;
    private UUID batchBId;
    private UUID photoBatchBId;

    private String agencyToken;

    @BeforeEach
    void setUp() throws Exception {
        if (roleRepository.findByRoleName("SCHOOL").isEmpty()) {
            roleRepository.save(new Role(null, "SCHOOL", "School role"));
        }
        if (roleRepository.findByRoleName("PRINTING_AGENCY").isEmpty()) {
            roleRepository.save(new Role(null, "PRINTING_AGENCY", "Agency role"));
        }

        // 1. Create School A and User A
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        var sA = schoolService.createSchool(new SchoolRequest(
                "School Alpha Import " + suffixA,
                "IMPA-" + suffixA,
                "100 Alpha Lane",
                "Alpha City",
                "Alpha State",
                "110001",
                "alpha_imp_" + suffixA + "@school.edu",
                "9811111101",
                "ACTIVE"
        ));
        schoolAId = sA.id();
        schoolService.createSchoolUser(schoolAId, new CreateOrganizationUserRequest(
                "Principal Alpha",
                "principal_imp_a_" + suffixA + "@school.edu",
                "9811111101",
                "Password123!",
                "Principal"
        ));

        MvcResult loginA = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "principal_imp_a_" + suffixA + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        schoolAToken = objectMapper.readTree(loginA.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 2. Create School B and User B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        var sB = schoolService.createSchool(new SchoolRequest(
                "School Beta Import " + suffixB,
                "IMPB-" + suffixB,
                "200 Beta Road",
                "Beta City",
                "Beta State",
                "220002",
                "beta_imp_" + suffixB + "@school.edu",
                "9822222202",
                "ACTIVE"
        ));
        schoolBId = sB.id();
        schoolService.createSchoolUser(schoolBId, new CreateOrganizationUserRequest(
                "Principal Beta",
                "principal_imp_b_" + suffixB + "@school.edu",
                "9822222202",
                "Password123!",
                "Principal"
        ));

        MvcResult loginB = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "principal_imp_b_" + suffixB + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        schoolBToken = objectMapper.readTree(loginB.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 3. Create Printing Agency and User
        String suffixAgency = UUID.randomUUID().toString().substring(0, 8);
        var agency = agencyService.createAgency(new AgencyRequest(
                "Agency Import Sec " + suffixAgency,
                "Contact " + suffixAgency,
                "agency_imp_" + suffixAgency + "@print.com",
                "9833333303",
                "Industrial Area",
                5000,
                "PVC",
                "ACTIVE"
        ));
        agencyService.createAgencyUser(agency.id(), new CreateOrganizationUserRequest(
                "Agency Supervisor",
                "supervisor_imp_" + suffixAgency + "@print.com",
                "9833333303",
                "Password123!",
                "Supervisor"
        ));

        MvcResult loginAgency = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "supervisor_imp_" + suffixAgency + "@print.com",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(loginAgency.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 4. Create Student for School B
        var studentResp = studentService.createStudent(schoolBId, new StudentRequest(
                "ADM-SEC-B01",
                "Student Beta 1",
                "Father B",
                "Mother B",
                LocalDate.of(2010, 5, 15),
                "FEMALE",
                "B+",
                "10th",
                "A",
                "01",
                "9822222203",
                "9822222204",
                "Beta Address",
                null,
                "ACTIVE"
        ));
        studentBId = studentResp.id();

        // 5. Create StudentImportBatch for School B
        School schoolB = schoolRepository.findById(schoolBId).orElseThrow();
        User userB = userRepository.findByEmail("principal_imp_b_" + suffixB + "@school.edu").orElseThrow();

        StudentImportBatch batchB = new StudentImportBatch();
        batchB.setSchool(schoolB);
        batchB.setUploadedBy(userB);
        batchB.setFileType("CSV");
        batchB.setTotalRows(1);
        batchB.setValidRows(1);
        batchB.setErrorRows(0);
        batchB.setImportedRows(0);
        batchB.setImportStatus("VALIDATED");
        batchB = studentImportBatchRepository.save(batchB);
        batchBId = batchB.getId();

        // 6. Create PhotoImportBatch for School B
        PhotoImportBatch photoBatchB = new PhotoImportBatch();
        photoBatchB.setSchool(schoolB);
        photoBatchB.setUploadedBy(userB);
        photoBatchB.setTotalFiles(1);
        photoBatchB.setMatchedFiles(1);
        photoBatchB.setUnmatchedFiles(0);
        photoBatchB.setDuplicateFiles(0);
        photoBatchB.setImportStatus("COMPLETED");
        photoBatchB = photoImportBatchRepository.save(photoBatchB);
        photoBatchBId = photoBatchB.getId();
    }

    // ==========================================
    // Cross-School Tenant Isolation Tests (School A -> School B)
    // ==========================================

    @Test
    @DisplayName("Non-negotiable Rule 2: School A cannot view School B's import batches (returns 404 RESOURCE_NOT_FOUND)")
    void schoolACannotViewSchoolBImportBatch() throws Exception {
        // School B can view its own batch (sanity check)
        mockMvc.perform(get("/api/v1/school/students/import/" + batchBId)
                        .header("Authorization", "Bearer " + schoolBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId", is(batchBId.toString())));

        // School A attempting to view School B's batch must receive 404
        mockMvc.perform(get("/api/v1/school/students/import/" + batchBId)
                        .header("Authorization", "Bearer " + schoolAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 2: School A cannot confirm School B's import batches (returns 404 RESOURCE_NOT_FOUND)")
    void schoolACannotConfirmSchoolBImportBatch() throws Exception {
        mockMvc.perform(post("/api/v1/school/students/import/" + batchBId + "/confirm")
                        .header("Authorization", "Bearer " + schoolAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 2: School A cannot download School B's import error report (returns 404 RESOURCE_NOT_FOUND)")
    void schoolACannotDownloadSchoolBErrorReport() throws Exception {
        mockMvc.perform(get("/api/v1/school/students/import/" + batchBId + "/errors")
                        .header("Authorization", "Bearer " + schoolAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 2: School A cannot view School B's photo upload batches (returns 404 RESOURCE_NOT_FOUND)")
    void schoolACannotViewSchoolBPhotoBatch() throws Exception {
        // School B can view its own photo batch (sanity check)
        mockMvc.perform(get("/api/v1/school/photos/bulk-upload/" + photoBatchBId)
                        .header("Authorization", "Bearer " + schoolBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.batchId", is(photoBatchBId.toString())));

        // School A attempting to view School B's photo batch receives 404
        mockMvc.perform(get("/api/v1/school/photos/bulk-upload/" + photoBatchBId)
                        .header("Authorization", "Bearer " + schoolAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 2: School A cannot upload/replace photo for School B's student (returns 404 RESOURCE_NOT_FOUND)")
    void schoolACannotUploadPhotoForSchoolBStudent() throws Exception {
        MockMultipartFile photo = new MockMultipartFile(
                "file",
                "student_photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy jpeg content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/school/students/" + studentBId + "/photo")
                        .file(photo)
                        .header("Authorization", "Bearer " + schoolAToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    // ==========================================
    // Role Isolation Tests: PRINTING_AGENCY Role
    // ==========================================

    @Test
    @DisplayName("Non-negotiable Rule 1: Printing Agency receives 403 on import-template endpoint")
    void agencyCannotAccessImportTemplate() throws Exception {
        mockMvc.perform(get("/api/v1/school/students/import-template")
                        .header("Authorization", "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Printing Agency receives 403 on student file upload endpoint")
    void agencyCannotUploadStudentImportFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students.csv",
                "text/csv",
                "Admission Number,Student Name,Class,Section\nADM001,Student,10,A\n".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart("/api/v1/school/students/import")
                        .file(file)
                        .header("Authorization", "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Printing Agency receives 403 on get import batch details endpoint")
    void agencyCannotGetStudentImportBatchDetails() throws Exception {
        mockMvc.perform(get("/api/v1/school/students/import/" + batchBId)
                        .header("Authorization", "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Printing Agency receives 403 on confirm import batch endpoint")
    void agencyCannotConfirmStudentImportBatch() throws Exception {
        mockMvc.perform(post("/api/v1/school/students/import/" + batchBId + "/confirm")
                        .header("Authorization", "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Printing Agency receives 403 on download error report endpoint")
    void agencyCannotDownloadStudentImportErrors() throws Exception {
        mockMvc.perform(get("/api/v1/school/students/import/" + batchBId + "/errors")
                        .header("Authorization", "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Printing Agency receives 403 on bulk photo upload endpoint")
    void agencyCannotBulkUploadPhotos() throws Exception {
        byte[] zipBytes = createZipBytes("10th_A_01.jpg", "image data".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile zipFile = new MockMultipartFile(
                "file",
                "photos.zip",
                "application/zip",
                zipBytes
        );

        mockMvc.perform(multipart("/api/v1/school/photos/bulk-upload")
                        .file(zipFile)
                        .header("Authorization", "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Printing Agency receives 403 on photo batch report endpoint")
    void agencyCannotGetPhotoBatchReport() throws Exception {
        mockMvc.perform(get("/api/v1/school/photos/bulk-upload/" + photoBatchBId)
                        .header("Authorization", "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Printing Agency receives 403 on student photo assignment endpoint")
    void agencyCannotAssignStudentPhoto() throws Exception {
        MockMultipartFile photo = new MockMultipartFile(
                "file",
                "student_photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "dummy jpeg content".getBytes(StandardCharsets.UTF_8)
        );

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/v1/school/students/" + studentBId + "/photo")
                        .file(photo)
                        .header("Authorization", "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    // ==========================================
    // Zip-Slip Path Traversal Vulnerability Test
    // ==========================================

    @Test
    @DisplayName("Security Requirement 4: Bulk photo upload rejects malicious Zip-Slip archive with 400 VALIDATION_FAILED")
    void bulkPhotoUploadRejectsZipSlipAttack() throws Exception {
        byte[] maliciousZipBytes = createZipBytes("../../etc/evil.jpg", "malicious payload".getBytes(StandardCharsets.UTF_8));
        MockMultipartFile maliciousZip = new MockMultipartFile(
                "file",
                "exploit.zip",
                "application/zip",
                maliciousZipBytes
        );

        mockMvc.perform(multipart("/api/v1/school/photos/bulk-upload")
                        .file(maliciousZip)
                        .header("Authorization", "Bearer " + schoolBToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.message", containsString("Zip traversal")));
    }

    private byte[] createZipBytes(String entryName, byte[] content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(content);
            zos.closeEntry();
        }
        return baos.toByteArray();
    }
}
