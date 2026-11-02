package com.platform.idcard.student;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.student.dto.StudentRequest;
import com.platform.idcard.student.entity.Student;
import com.platform.idcard.student.repository.StudentRepository;
import com.platform.idcard.student.service.StudentService;
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
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PhotoImportIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StudentRepository studentRepository;

    private String school1Token;
    private UUID school1Id;
    private String school2Token;
    private UUID school2Id;

    private UUID student1Id;
    private UUID student2Id;
    private UUID student3Id;
    private UUID student4Id;

    @BeforeEach
    void setUp() throws Exception {
        if (roleRepository.findByRoleName("SCHOOL").isEmpty()) {
            roleRepository.save(new Role(null, "SCHOOL", "School role"));
        }

        // Setup School 1
        String suffix1 = UUID.randomUUID().toString().substring(0, 8);
        var s1 = schoolService.createSchool(new SchoolRequest(
                "School Photo Test " + suffix1,
                "SCH1-P-" + suffix1,
                "100 Photo Blvd",
                "Metropolis",
                "State A",
                "500001",
                "photo_s1_" + suffix1 + "@school.edu",
                "9876543201",
                "ACTIVE"
        ));
        school1Id = s1.id();
        schoolService.createSchoolUser(school1Id, new CreateOrganizationUserRequest(
                "Photo In-Charge 1",
                "user_photo1_" + suffix1 + "@school.edu",
                "9876543201",
                "Password123!",
                "Coordinator"
        ));

        MvcResult login1 = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "user_photo1_" + suffix1 + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        school1Token = objectMapper.readTree(login1.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Setup School 2
        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        var s2 = schoolService.createSchool(new SchoolRequest(
                "School Photo Isolation " + suffix2,
                "SCH2-P-" + suffix2,
                "200 Isolation Way",
                "Othertown",
                "State B",
                "500002",
                "photo_s2_" + suffix2 + "@school.edu",
                "9876543202",
                "ACTIVE"
        ));
        school2Id = s2.id();
        schoolService.createSchoolUser(school2Id, new CreateOrganizationUserRequest(
                "Photo In-Charge 2",
                "user_photo2_" + suffix2 + "@school.edu",
                "9876543202",
                "Password123!",
                "Coordinator"
        ));

        MvcResult login2 = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "user_photo2_" + suffix2 + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        school2Token = objectMapper.readTree(login2.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Pre-create known students in School 1:
        // Student 1: 10th-A-1, ADM-P-001
        var st1 = studentService.createStudent(school1Id, new StudentRequest(
                "ADM-P-001",
                "Priya Sharma",
                "Suresh Sharma",
                "Anita Sharma",
                LocalDate.of(2010, 3, 10),
                "FEMALE",
                "O+",
                "10th",
                "A",
                "1",
                "Sector 1, City",
                "9876543210",
                "9876543211",
                null,
                "ACTIVE"
        ));
        student1Id = st1.id();

        // Student 2: 10th-B-2, ADM-P-002
        var st2 = studentService.createStudent(school1Id, new StudentRequest(
                "ADM-P-002",
                "Rohan Verma",
                "Kishore Verma",
                "Sunita Verma",
                LocalDate.of(2010, 5, 20),
                "MALE",
                "B+",
                "10th",
                "B",
                "2",
                "Sector 2, City",
                "9876543220",
                "9876543221",
                null,
                "ACTIVE"
        ));
        student2Id = st2.id();

        // Student 3: 11th-A-3, ADM-P-003
        var st3 = studentService.createStudent(school1Id, new StudentRequest(
                "ADM-P-003",
                "Sanya Gupta",
                "Manoj Gupta",
                "Rekha Gupta",
                LocalDate.of(2009, 8, 14),
                "FEMALE",
                "A+",
                "11th",
                "A",
                "3",
                "Sector 3, City",
                "9876543230",
                "9876543231",
                null,
                "ACTIVE"
        ));
        student3Id = st3.id();

        // Student 4: 10th-A-4, ADM-P-004
        var st4 = studentService.createStudent(school1Id, new StudentRequest(
                "ADM-P-004",
                "Vikram Singh",
                "Rajesh Singh",
                "Geeta Singh",
                LocalDate.of(2010, 11, 28),
                "MALE",
                "AB+",
                "10th",
                "A",
                "4",
                "Sector 4, City",
                "9876543240",
                "9876543241",
                null,
                "ACTIVE"
        ));
        student4Id = st4.id();
    }

    @Test
    @DisplayName("Bulk upload ZIP with Option B (class_sec_roll), Option C (admission), Option A (unique roll), and unmatched photo")
    void bulkUploadPhotos_matchesMultiOptionAndReportsUnmatched() throws Exception {
        byte[] jpegBytes = createDummyImageBytes("jpg", Color.BLUE);
        byte[] pngBytes = createDummyImageBytes("png", Color.RED);

        Map<String, byte[]> zipFiles = new LinkedHashMap<>();
        // Option B: class_sec_roll -> 10th_A_1.jpg matches Student 1 (10th-A-1)
        zipFiles.put("10th_A_1.jpg", jpegBytes);
        // Option C: admission number -> ADM-P-002.png matches Student 2 (ADM-P-002)
        zipFiles.put("ADM-P-002.png", pngBytes);
        // Option A / unique roll -> 3.jpg matches Student 3 (unique roll 3 in school)
        zipFiles.put("3.jpg", jpegBytes);
        // Unmatched photo -> UNKNOWN_PHOTO.jpg has no student
        zipFiles.put("UNKNOWN_PHOTO.jpg", jpegBytes);

        byte[] zipArchive = createZipArchive(zipFiles);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photos_mixed.zip",
                "application/zip",
                zipArchive
        );

        MvcResult result = mockMvc.perform(multipart("/api/v1/school/photos/bulk-upload")
                        .file(file)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.batchId", notNullValue()))
                .andExpect(jsonPath("$.data.totalFiles", is(4)))
                .andExpect(jsonPath("$.data.matchedCount", is(3)))
                .andExpect(jsonPath("$.data.unmatchedCount", is(1)))
                .andExpect(jsonPath("$.data.duplicateCount", is(0)))
                .andExpect(jsonPath("$.data.importStatus", is("COMPLETED")))
                .andExpect(jsonPath("$.data.results", hasSize(4)))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        UUID batchId = UUID.fromString(json.path("batchId").asText());

        // Verify Student 1 was matched
        JsonNode r1 = findResultByFileName(json.path("results"), "10th_A_1.jpg");
        assertThat(r1).isNotNull();
        assertThat(r1.path("matchStatus").asText()).isEqualTo("MATCHED");
        assertThat(r1.path("studentId").asText()).isEqualTo(student1Id.toString());
        assertThat(r1.path("studentName").asText()).isEqualTo("Priya Sharma");

        // Verify Student 2 was matched
        JsonNode r2 = findResultByFileName(json.path("results"), "ADM-P-002.png");
        assertThat(r2).isNotNull();
        assertThat(r2.path("matchStatus").asText()).isEqualTo("MATCHED");
        assertThat(r2.path("studentId").asText()).isEqualTo(student2Id.toString());
        assertThat(r2.path("studentName").asText()).isEqualTo("Rohan Verma");

        // Verify Student 3 was matched
        JsonNode r3 = findResultByFileName(json.path("results"), "3.jpg");
        assertThat(r3).isNotNull();
        assertThat(r3.path("matchStatus").asText()).isEqualTo("MATCHED");
        assertThat(r3.path("studentId").asText()).isEqualTo(student3Id.toString());
        assertThat(r3.path("studentName").asText()).isEqualTo("Sanya Gupta");

        // Verify UNKNOWN_PHOTO was not matched
        JsonNode r4 = findResultByFileName(json.path("results"), "UNKNOWN_PHOTO.jpg");
        assertThat(r4).isNotNull();
        assertThat(r4.path("matchStatus").asText()).isEqualTo("NO_STUDENT_FOUND");

        // Verify GET /api/v1/school/photos/bulk-upload/{batchId} returns identical match stats
        mockMvc.perform(get("/api/v1/school/photos/bulk-upload/" + batchId)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.batchId", is(batchId.toString())))
                .andExpect(jsonPath("$.data.totalFiles", is(4)))
                .andExpect(jsonPath("$.data.matchedCount", is(3)))
                .andExpect(jsonPath("$.data.unmatchedCount", is(1)))
                .andExpect(jsonPath("$.data.results", hasSize(4)));

        // Verify student records in database now have photoFileId assigned
        Student s1 = studentRepository.findById(student1Id).orElseThrow();
        assertThat(s1.getPhotoFileId()).isNotNull();

        Student s2 = studentRepository.findById(student2Id).orElseThrow();
        assertThat(s2.getPhotoFileId()).isNotNull();

        Student s3 = studentRepository.findById(student3Id).orElseThrow();
        assertThat(s3.getPhotoFileId()).isNotNull();

        // Verify photo file can be fetched via GET /api/v1/files/{fileId}
        MvcResult fileResult1 = mockMvc.perform(get("/api/v1/files/" + s1.getPhotoFileId())
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/jpeg"))
                .andReturn();
        assertThat(fileResult1.getResponse().getContentAsByteArray()).isNotEmpty();

        MvcResult fileResult2 = mockMvc.perform(get("/api/v1/files/" + s2.getPhotoFileId())
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andReturn();
        assertThat(fileResult2.getResponse().getContentAsByteArray()).isNotEmpty();

        // Cross-tenant access: School 2 cannot view School 1's photo file (403 Forbidden)
        mockMvc.perform(get("/api/v1/files/" + s1.getPhotoFileId())
                        .header("Authorization", "Bearer " + school2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Bulk upload ZIP using Option A with explicit className and section parameters")
    void bulkUploadPhotos_withOptionA_parametersMatchClassAndRoll() throws Exception {
        byte[] jpegBytes = createDummyImageBytes("jpg", Color.GREEN);

        Map<String, byte[]> zipFiles = new LinkedHashMap<>();
        // In Option A, filename is roll number with leading zero e.g. "04.jpg"
        zipFiles.put("04.jpg", jpegBytes);

        byte[] zipArchive = createZipArchive(zipFiles);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "class10_secA_photos.zip",
                "application/zip",
                zipArchive
        );

        mockMvc.perform(multipart("/api/v1/school/photos/bulk-upload")
                        .file(file)
                        .param("className", "10th")
                        .param("section", "A")
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalFiles", is(1)))
                .andExpect(jsonPath("$.data.matchedCount", is(1)))
                .andExpect(jsonPath("$.data.unmatchedCount", is(0)))
                .andExpect(jsonPath("$.data.results[0].fileName", is("04.jpg")))
                .andExpect(jsonPath("$.data.results[0].matchStatus", is("MATCHED")))
                .andExpect(jsonPath("$.data.results[0].studentId", is(student4Id.toString())))
                .andExpect(jsonPath("$.data.results[0].studentName", is("Vikram Singh")));

        Student s4 = studentRepository.findById(student4Id).orElseThrow();
        assertThat(s4.getPhotoFileId()).isNotNull();
    }

    @Test
    @DisplayName("PUT /api/v1/school/students/{id}/photo uploads and updates single student photo")
    void uploadStudentPhoto_singleUpload_updatesPhotoFileId() throws Exception {
        byte[] pngBytes = createDummyImageBytes("png", Color.MAGENTA);

        MockMultipartFile photoFile = new MockMultipartFile(
                "file",
                "single_student_photo.png",
                "image/png",
                pngBytes
        );

        MockMultipartHttpServletRequestBuilder builder = (MockMultipartHttpServletRequestBuilder)
                multipart("/api/v1/school/students/" + student4Id + "/photo")
                        .file(photoFile)
                        .header("Authorization", "Bearer " + school1Token);
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        MvcResult putResult = mockMvc.perform(builder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.studentId", is(student4Id.toString())))
                .andExpect(jsonPath("$.data.photoFileId", notNullValue()))
                .andExpect(jsonPath("$.data.photoUrl", containsString("/api/v1/files/")))
                .andExpect(jsonPath("$.data.message", containsString("Photo uploaded and assigned successfully")))
                .andReturn();

        UUID updatedPhotoFileId = UUID.fromString(objectMapper.readTree(putResult.getResponse().getContentAsString())
                .path("data").path("photoFileId").asText());

        // Verify student entity has new photo file id
        Student updatedStudent = studentRepository.findById(student4Id).orElseThrow();
        assertThat(updatedStudent.getPhotoFileId()).isEqualTo(updatedPhotoFileId);

        // Verify new photo is downloadable
        mockMvc.perform(get("/api/v1/files/" + updatedPhotoFileId)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"));
    }

    @Test
    @DisplayName("Bulk upload ZIP handles duplicate photos for same student in archive")
    void bulkUploadPhotos_duplicatePhotoInZip_markedAsDuplicate() throws Exception {
        byte[] imgBytes = createDummyImageBytes("jpg", Color.CYAN);

        Map<String, byte[]> zipFiles = new LinkedHashMap<>();
        // Two files matching the exact same student (Student 1)
        zipFiles.put("10th_A_1.jpg", imgBytes);
        zipFiles.put("ADM-P-001.jpg", imgBytes);

        byte[] zipArchive = createZipArchive(zipFiles);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "duplicates.zip",
                "application/zip",
                zipArchive
        );

        mockMvc.perform(multipart("/api/v1/school/photos/bulk-upload")
                        .file(file)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFiles", is(2)))
                .andExpect(jsonPath("$.data.matchedCount", is(1)))
                .andExpect(jsonPath("$.data.duplicateCount", is(1)))
                .andExpect(jsonPath("$.data.results[1].matchStatus", is("DUPLICATE_PHOTO")));
    }

    @Test
    @DisplayName("Bulk upload ZIP handles unsupported file formats inside ZIP")
    void bulkUploadPhotos_unsupportedFormatInZip_markedAsBadImage() throws Exception {
        byte[] imgBytes = createDummyImageBytes("jpg", Color.YELLOW);

        Map<String, byte[]> zipFiles = new LinkedHashMap<>();
        zipFiles.put("10th_A_1.jpg", imgBytes);
        zipFiles.put("notes.txt", "Plain text file".getBytes(StandardCharsets.UTF_8));

        byte[] zipArchive = createZipArchive(zipFiles);

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "with_bad_file.zip",
                "application/zip",
                zipArchive
        );

        mockMvc.perform(multipart("/api/v1/school/photos/bulk-upload")
                        .file(file)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalFiles", is(2)))
                .andExpect(jsonPath("$.data.matchedCount", is(1)))
                .andExpect(jsonPath("$.data.unmatchedCount", is(1)))
                .andExpect(jsonPath("$.data.results[1].fileName", is("notes.txt")))
                .andExpect(jsonPath("$.data.results[1].matchStatus", is("BAD_IMAGE")))
                .andExpect(jsonPath("$.data.results[1].message", containsString("Unsupported image format")));
    }

    @Test
    @DisplayName("Photo upload validation failures on empty file, non-zip file, and single photo format error")
    void photoUpload_validationFailures_returnBadRequest() throws Exception {
        // 1. Empty zip
        MockMultipartFile emptyZip = new MockMultipartFile(
                "file",
                "empty.zip",
                "application/zip",
                new byte[0]
        );
        mockMvc.perform(multipart("/api/v1/school/photos/bulk-upload")
                        .file(emptyZip)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));

        // 2. Non-zip upload
        MockMultipartFile nonZip = new MockMultipartFile(
                "file",
                "photos.tar",
                "application/x-tar",
                "Fake tar archive".getBytes(StandardCharsets.UTF_8)
        );
        mockMvc.perform(multipart("/api/v1/school/photos/bulk-upload")
                        .file(nonZip)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));

        // 3. Single photo upload with unsupported file extension
        MockMultipartFile badSinglePhoto = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "PDF file content".getBytes(StandardCharsets.UTF_8)
        );
        MockMultipartHttpServletRequestBuilder builder = (MockMultipartHttpServletRequestBuilder)
                multipart("/api/v1/school/students/" + student1Id + "/photo")
                        .file(badSinglePhoto)
                        .header("Authorization", "Bearer " + school1Token);
        builder.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        mockMvc.perform(builder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));

        // 4. Single photo upload for non-existent student ID
        UUID nonExistentStudentId = UUID.randomUUID();
        MockMultipartFile validPhoto = new MockMultipartFile(
                "file",
                "valid.jpg",
                "image/jpeg",
                createDummyImageBytes("jpg", Color.BLACK)
        );
        MockMultipartHttpServletRequestBuilder builderNotFound = (MockMultipartHttpServletRequestBuilder)
                multipart("/api/v1/school/students/" + nonExistentStudentId + "/photo")
                        .file(validPhoto)
                        .header("Authorization", "Bearer " + school1Token);
        builderNotFound.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        mockMvc.perform(builderNotFound)
                .andExpect(status().isNotFound());

        // 5. School 2 cannot upload photo to School 1's student
        MockMultipartHttpServletRequestBuilder builderForbidden = (MockMultipartHttpServletRequestBuilder)
                multipart("/api/v1/school/students/" + student1Id + "/photo")
                        .file(validPhoto)
                        .header("Authorization", "Bearer " + school2Token);
        builderForbidden.with(request -> {
            request.setMethod("PUT");
            return request;
        });

        mockMvc.perform(builderForbidden)
                .andExpect(status().isNotFound());
    }

    private byte[] createDummyImageBytes(String format, Color color) throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 10, 10);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    private byte[] createZipArchive(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                ZipEntry zipEntry = new ZipEntry(entry.getKey());
                zos.putNextEntry(zipEntry);
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private JsonNode findResultByFileName(JsonNode resultsNode, String fileName) {
        if (resultsNode != null && resultsNode.isArray()) {
            for (JsonNode item : resultsNode) {
                if (fileName.equals(item.path("fileName").asText())) {
                    return item;
                }
            }
        }
        return null;
    }
}
