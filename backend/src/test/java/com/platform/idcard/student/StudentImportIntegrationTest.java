package com.platform.idcard.student;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.student.entity.Student;
import com.platform.idcard.student.entity.StudentImportBatch;
import com.platform.idcard.student.repository.StudentImportBatchRepository;
import com.platform.idcard.student.repository.StudentRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private StudentImportBatchRepository batchRepository;

    private String school1Token;
    private UUID school1Id;
    private String school2Token;
    private UUID school2Id;

    @BeforeEach
    void setUp() throws Exception {
        if (roleRepository.findByRoleName("SCHOOL").isEmpty()) {
            roleRepository.save(new Role(null, "SCHOOL", "School role"));
        }

        // Setup School 1
        String suffix1 = UUID.randomUUID().toString().substring(0, 8);
        var s1 = schoolService.createSchool(new SchoolRequest(
                "School Import Test " + suffix1,
                "SCH1-" + suffix1,
                "123 School Way",
                "Metro City",
                "Stateland",
                "560001",
                "import_s1_" + suffix1 + "@school.edu",
                "9876543201",
                "ACTIVE"
        ));
        school1Id = s1.id();
        schoolService.createSchoolUser(school1Id, new CreateOrganizationUserRequest(
                "Import Coordinator 1",
                "user_imp1_" + suffix1 + "@school.edu",
                "9876543201",
                "Password123!",
                "Coordinator"
        ));

        MvcResult login1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "user_imp1_" + suffix1 + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        school1Token = objectMapper.readTree(login1.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Setup School 2 for cross-tenant isolation testing
        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        var s2 = schoolService.createSchool(new SchoolRequest(
                "School Isolation " + suffix2,
                "SCH2-" + suffix2,
                "456 Other Way",
                "Other City",
                "Stateland",
                "560002",
                "import_s2_" + suffix2 + "@school.edu",
                "9876543202",
                "ACTIVE"
        ));
        school2Id = s2.id();
        schoolService.createSchoolUser(school2Id, new CreateOrganizationUserRequest(
                "Import Coordinator 2",
                "user_imp2_" + suffix2 + "@school.edu",
                "9876543202",
                "Password123!",
                "Coordinator"
        ));

        MvcResult login2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "user_imp2_" + suffix2 + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        school2Token = objectMapper.readTree(login2.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("GET /api/v1/school/students/import-template?format=xlsx returns 200 OK, valid headers, and Excel sheet")
    void downloadImportTemplate_xlsx_returnsValidExcelFile() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/school/students/import-template")
                        .param("format", "xlsx")
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"student_import_template.xlsx\""))
                .andReturn();

        byte[] xlsxBytes = result.getResponse().getContentAsByteArray();
        assertThat(xlsxBytes).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            Sheet sheet = workbook.getSheet("Students Template");
            assertThat(sheet).isNotNull();
            Row headerRow = sheet.getRow(0);
            assertThat(headerRow).isNotNull();
            assertThat(headerRow.getCell(0).getStringCellValue()).isEqualTo("Admission Number*");
            assertThat(headerRow.getCell(1).getStringCellValue()).isEqualTo("Student Name*");
            assertThat(headerRow.getCell(2).getStringCellValue()).isEqualTo("Class*");
            assertThat(headerRow.getCell(3).getStringCellValue()).isEqualTo("Section*");
            assertThat(headerRow.getCell(4).getStringCellValue()).isEqualTo("Roll Number*");
            assertThat(sheet.getPhysicalNumberOfRows()).isGreaterThanOrEqualTo(2);
        }
    }

    @Test
    @DisplayName("GET /api/v1/school/students/import-template?format=csv returns 200 OK and CSV template with standard headers")
    void downloadImportTemplate_csv_returnsValidCsvFile() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/school/students/import-template")
                        .param("format", "csv")
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"student_import_template.csv\""))
                .andReturn();

        String csvContent = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csvContent).isNotEmpty();
        assertThat(csvContent).contains("Admission Number*");
        assertThat(csvContent).contains("Student Name*");
        assertThat(csvContent).contains("Class*");
        assertThat(csvContent).contains("Section*");
        assertThat(csvContent).contains("Roll Number*");
        assertThat(csvContent).contains("ADM-2026-001");
    }

    @Test
    @DisplayName("POST /api/v1/school/students/import processes CSV, reports validation summary and preview rows")
    void uploadImportFile_withValidAndInvalidRows_returnsValidationReport() throws Exception {
        String csvData = """
                Admission Number*,Student Name*,Class*,Section*,Roll Number*,Father's Name,Mother's Name,Date of Birth,Gender,Blood Group,Mobile,Emergency Contact,Address
                ADM-001,Alice Smith,10th,A,1,Bob Smith,Carol Smith,2010-01-15,FEMALE,O+,9876543210,9876543211,123 Elm St
                ADM-002,Bob Jones,10th,A,2,David Jones,Eva Jones,2010-02-20,MALE,A+,9876543212,9876543213,456 Oak St
                ADM-003,,10th,A,3,Frank Miller,Grace Miller,2010-03-25,MALE,B+,9876543214,9876543215,789 Pine St
                ADM-004,Charlie Brown,10th,A,4,George Brown,Helen Brown,2010-04-10,MALE,Z_INVALID,9876543216,9876543217,101 Maple St
                ADM-005,Dan White,10th,A,1,Ian White,Jane White,2010-05-05,MALE,AB+,9876543218,9876543219,202 Birch St
                ADM-001,Duplicate Alice,10th,B,5,Bob Smith,Carol Smith,2010-06-18,FEMALE,O+,9876543210,9876543211,123 Elm St
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students_batch_test.csv",
                "text/csv",
                csvData.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/school/students/import")
                        .file(file)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.batchId", notNullValue()))
                .andExpect(jsonPath("$.data.totalRows", is(6)))
                .andExpect(jsonPath("$.data.validRows", is(2)))
                .andExpect(jsonPath("$.data.errorRows", is(4)))
                .andExpect(jsonPath("$.data.importStatus", is("VALIDATED")))
                .andExpect(jsonPath("$.data.previewRows", hasSize(6)))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID batchId = UUID.fromString(responseNode.path("data").path("batchId").asText());

        // Verify batch detail via GET /api/v1/school/students/import/{batchId}
        mockMvc.perform(get("/api/v1/school/students/import/" + batchId)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.batchId", is(batchId.toString())))
                .andExpect(jsonPath("$.data.totalRows", is(6)))
                .andExpect(jsonPath("$.data.validRows", is(2)))
                .andExpect(jsonPath("$.data.errorRows", is(4)))
                .andExpect(jsonPath("$.data.importStatus", is("VALIDATED")))
                .andExpect(jsonPath("$.data.rows", hasSize(6)))
                .andExpect(jsonPath("$.pagination.totalElements", is(6)));

        // Test status filter VALID
        mockMvc.perform(get("/api/v1/school/students/import/" + batchId)
                        .param("status", "VALID")
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows", hasSize(2)))
                .andExpect(jsonPath("$.data.rows[0].admissionNumber", is("ADM-001")))
                .andExpect(jsonPath("$.data.rows[0].validationStatus", is("VALID")))
                .andExpect(jsonPath("$.data.rows[1].admissionNumber", is("ADM-002")))
                .andExpect(jsonPath("$.data.rows[1].validationStatus", is("VALID")));

        // Test status filter ERROR
        mockMvc.perform(get("/api/v1/school/students/import/" + batchId)
                        .param("status", "ERROR")
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.rows", hasSize(4)))
                .andExpect(jsonPath("$.data.rows[0].rowNumber", is(4))) // Missing name row
                .andExpect(jsonPath("$.data.rows[0].errorMessage", containsString("Student name is required")))
                .andExpect(jsonPath("$.data.rows[1].errorMessage", containsString("Invalid blood group")))
                .andExpect(jsonPath("$.data.rows[2].errorMessage", containsString("duplicate")))
                .andExpect(jsonPath("$.data.rows[3].errorMessage", containsString("Duplicate admission number")));
    }

    @Test
    @DisplayName("POST /api/v1/school/students/import/{batchId}/confirm commits valid rows and skips error rows")
    void confirmImport_persistsOnlyValidStudents() throws Exception {
        String csvData = """
                Admission Number*,Student Name*,Class*,Section*,Roll Number*,Father's Name,Mother's Name,Date of Birth,Gender,Blood Group,Mobile,Emergency Contact,Address
                ADM-VAL-01,Valid Student One,9th,B,10,Parent One,Parent Two,2011-04-12,MALE,B+,9876543201,9876543202,Road 1
                ADM-VAL-02,Valid Student Two,9th,B,11,Parent Three,Parent Four,2011-07-25,FEMALE,AB+,9876543203,9876543204,Road 2
                ADM-ERR-03,,9th,B,12,Parent Five,Parent Six,2011-09-01,MALE,O+,9876543205,9876543206,Road 3
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "students_confirm_test.csv",
                "text/csv",
                csvData.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/school/students/import")
                        .file(file)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andReturn();

        UUID batchId = UUID.fromString(objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("batchId").asText());

        // Confirm batch
        mockMvc.perform(post("/api/v1/school/students/import/" + batchId + "/confirm")
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.batchId", is(batchId.toString())))
                .andExpect(jsonPath("$.data.totalRows", is(3)))
                .andExpect(jsonPath("$.data.importedCount", is(2)))
                .andExpect(jsonPath("$.data.errorCount", is(1)))
                .andExpect(jsonPath("$.data.importStatus", is("COMPLETED_WITH_ERRORS")))
                .andExpect(jsonPath("$.data.message", containsString("Successfully imported 2 students. 1 rows were skipped due to errors.")));

        // Verify database persistence
        Optional<Student> s1 = studentRepository.findBySchoolIdAndAdmissionNumber(school1Id, "ADM-VAL-01");
        assertThat(s1).isPresent();
        assertThat(s1.get().getStudentName()).isEqualTo("Valid Student One");
        assertThat(s1.get().getClassName()).isEqualTo("9th");
        assertThat(s1.get().getSection()).isEqualTo("B");
        assertThat(s1.get().getRollNumber()).isEqualTo("10");

        Optional<Student> s2 = studentRepository.findBySchoolIdAndAdmissionNumber(school1Id, "ADM-VAL-02");
        assertThat(s2).isPresent();
        assertThat(s2.get().getStudentName()).isEqualTo("Valid Student Two");

        // Verify invalid row was not saved in students table
        Optional<Student> s3 = studentRepository.findBySchoolIdAndAdmissionNumber(school1Id, "ADM-ERR-03");
        assertThat(s3).isEmpty();

        // Second confirmation attempt on completed batch must fail with 400
        mockMvc.perform(post("/api/v1/school/students/import/" + batchId + "/confirm")
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("INVALID_ACTION")));
    }

    @Test
    @DisplayName("Import confirmation performs upsert: updates existing student record without duplicating")
    void confirmImport_upsertBehavior_updatesExistingStudentRecord() throws Exception {
        // Step 1: Initial import of student ADM-UPSERT-01
        String initialCsv = """
                Admission Number*,Student Name*,Class*,Section*,Roll Number*,Father's Name,Mother's Name,Date of Birth,Gender,Blood Group,Mobile,Emergency Contact,Address
                ADM-UPSERT-01,Original Name,8th,C,5,Old Father,Old Mother,2012-05-10,MALE,O+,9111111111,9222222222,Old Address 123
                """;

        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "initial.csv",
                "text/csv",
                initialCsv.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult upload1 = mockMvc.perform(multipart("/api/v1/school/students/import")
                        .file(file1)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andReturn();

        UUID batchId1 = UUID.fromString(objectMapper.readTree(upload1.getResponse().getContentAsString())
                .path("data").path("batchId").asText());

        mockMvc.perform(post("/api/v1/school/students/import/" + batchId1 + "/confirm")
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importStatus", is("COMPLETED")));

        Student originalStudent = studentRepository.findBySchoolIdAndAdmissionNumber(school1Id, "ADM-UPSERT-01")
                .orElseThrow();
        UUID originalStudentId = originalStudent.getId();
        assertThat(originalStudent.getStudentName()).isEqualTo("Original Name");
        assertThat(originalStudent.getAddress()).isEqualTo("Old Address 123");

        // Step 2: Second import with same admission number ADM-UPSERT-01 but updated fields
        String updateCsv = """
                Admission Number*,Student Name*,Class*,Section*,Roll Number*,Father's Name,Mother's Name,Date of Birth,Gender,Blood Group,Mobile,Emergency Contact,Address
                ADM-UPSERT-01,Updated Full Name,8th,C,5,New Father,New Mother,2012-05-10,MALE,O+,9999999999,8888888888,New Renovated Address 456
                """;

        MockMultipartFile file2 = new MockMultipartFile(
                "file",
                "update.csv",
                "text/csv",
                updateCsv.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult upload2 = mockMvc.perform(multipart("/api/v1/school/students/import")
                        .file(file2)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validRows", is(1)))
                .andExpect(jsonPath("$.data.errorRows", is(0)))
                .andReturn();

        UUID batchId2 = UUID.fromString(objectMapper.readTree(upload2.getResponse().getContentAsString())
                .path("data").path("batchId").asText());

        mockMvc.perform(post("/api/v1/school/students/import/" + batchId2 + "/confirm")
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.importedCount", is(1)))
                .andExpect(jsonPath("$.data.importStatus", is("COMPLETED")));

        // Verify student record was updated, not duplicated
        Student updatedStudent = studentRepository.findBySchoolIdAndAdmissionNumber(school1Id, "ADM-UPSERT-01")
                .orElseThrow();
        assertThat(updatedStudent.getId()).isEqualTo(originalStudentId); // Same record ID
        assertThat(updatedStudent.getStudentName()).isEqualTo("Updated Full Name");
        assertThat(updatedStudent.getFatherName()).isEqualTo("New Father");
        assertThat(updatedStudent.getMotherName()).isEqualTo("New Mother");
        assertThat(updatedStudent.getAddress()).isEqualTo("New Renovated Address 456");
        assertThat(updatedStudent.getMobile()).isEqualTo("9999999999");
        assertThat(updatedStudent.getEmergencyContact()).isEqualTo("8888888888");

        // Verify total matching students count in DB for this school is exactly 1
        List<Student> allWithAdmission = studentRepository.findAll().stream()
                .filter(s -> s.getSchool().getId().equals(school1Id) && "ADM-UPSERT-01".equals(s.getAdmissionNumber()))
                .toList();
        assertThat(allWithAdmission).hasSize(1);
    }

    @Test
    @DisplayName("GET /api/v1/school/students/import/{batchId}/errors downloads CSV error report with field details")
    void downloadErrorReport_containsFieldLevelErrors() throws Exception {
        String csvData = """
                Admission Number*,Student Name*,Class*,Section*,Roll Number*,Father's Name,Mother's Name,Date of Birth,Gender,Blood Group,Mobile,Emergency Contact,Address
                ADM-ERR-11,,10th,A,11,Father,Mother,2010-01-01,MALE,O+,9876543210,9876543211,Addr
                ADM-ERR-12,Test Name,10th,A,12,Father,Mother,2010-01-01,MALE,X_POS,9876543210,9876543211,Addr
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "errors_test.csv",
                "text/csv",
                csvData.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/school/students/import")
                        .file(file)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andReturn();

        UUID batchId = UUID.fromString(objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("batchId").asText());

        MvcResult errorResult = mockMvc.perform(get("/api/v1/school/students/import/" + batchId + "/errors")
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"import_errors_" + batchId + ".csv\""))
                .andReturn();

        String errorCsv = errorResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(errorCsv).isNotEmpty();

        try (CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(new StringReader(errorCsv))) {
            List<CSVRecord> records = parser.getRecords();
            assertThat(records).hasSize(2);

            CSVRecord r1 = records.get(0);
            assertThat(r1.get("Row Number")).isEqualTo("2");
            assertThat(r1.get("Admission Number")).isEqualTo("ADM-ERR-11");
            assertThat(r1.get("Field Name")).isEqualTo("Student Name");
            assertThat(r1.get("Error Description")).contains("Student name is required");

            CSVRecord r2 = records.get(1);
            assertThat(r2.get("Row Number")).isEqualTo("3");
            assertThat(r2.get("Admission Number")).isEqualTo("ADM-ERR-12");
            assertThat(r2.get("Field Name")).isEqualTo("Blood Group");
            assertThat(r2.get("Error Description")).contains("Invalid blood group");
        }
    }

    @Test
    @DisplayName("Tenant isolation: School 2 cannot view or confirm School 1's import batch")
    void tenantIsolation_schoolCannotAccessOtherSchoolBatch() throws Exception {
        String csvData = """
                Admission Number*,Student Name*,Class*,Section*,Roll Number*,Father's Name,Mother's Name,Date of Birth,Gender,Blood Group,Mobile,Emergency Contact,Address
                ADM-ISO-01,Isolated Student,7th,A,1,Father,Mother,2013-02-02,MALE,O+,9876543210,9876543211,Address
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "isolated.csv",
                "text/csv",
                csvData.getBytes(StandardCharsets.UTF_8)
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/school/students/import")
                        .file(file)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andReturn();

        UUID school1BatchId = UUID.fromString(objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .path("data").path("batchId").asText());

        // School 2 attempts to get batch details
        mockMvc.perform(get("/api/v1/school/students/import/" + school1BatchId)
                        .header("Authorization", "Bearer " + school2Token))
                .andExpect(status().isNotFound());

        // School 2 attempts to confirm School 1's batch
        mockMvc.perform(post("/api/v1/school/students/import/" + school1BatchId + "/confirm")
                        .header("Authorization", "Bearer " + school2Token))
                .andExpect(status().isNotFound());

        // School 2 attempts to download error report of School 1's batch
        mockMvc.perform(get("/api/v1/school/students/import/" + school1BatchId + "/errors")
                        .header("Authorization", "Bearer " + school2Token))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Validation failure on empty file and unsupported file extension")
    void uploadImportFile_invalidRequests_returnBadRequest() throws Exception {
        // Empty file
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "empty.csv",
                "text/csv",
                new byte[0]
        );
        mockMvc.perform(multipart("/api/v1/school/students/import")
                        .file(emptyFile)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));

        // Unsupported extension (.pdf)
        MockMultipartFile unsupportedFile = new MockMultipartFile(
                "file",
                "students.pdf",
                "application/pdf",
                "Fake PDF content".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/school/students/import")
                        .file(unsupportedFile)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
    }
}
