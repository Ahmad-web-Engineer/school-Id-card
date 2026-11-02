package com.platform.idcard.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.student.dto.StudentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SchoolStudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private RoleRepository roleRepository;

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
                "School One " + suffix1,
                "SCH1-" + suffix1,
                "Address 1",
                "City 1",
                "State 1",
                "111111",
                "s1_" + suffix1 + "@school.edu",
                "9876543201",
                "ACTIVE"
        ));
        school1Id = s1.id();
        schoolService.createSchoolUser(school1Id, new CreateOrganizationUserRequest(
                "Principal One",
                "user1_" + suffix1 + "@school.edu",
                "9876543201",
                "Password123!",
                "Principal"
        ));

        MvcResult login1 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "user1_" + suffix1 + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        school1Token = objectMapper.readTree(login1.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Setup School 2
        String suffix2 = UUID.randomUUID().toString().substring(0, 8);
        var s2 = schoolService.createSchool(new SchoolRequest(
                "School Two " + suffix2,
                "SCH2-" + suffix2,
                "Address 2",
                "City 2",
                "State 2",
                "222222",
                "s2_" + suffix2 + "@school.edu",
                "9876543202",
                "ACTIVE"
        ));
        school2Id = s2.id();
        schoolService.createSchoolUser(school2Id, new CreateOrganizationUserRequest(
                "Principal Two",
                "user2_" + suffix2 + "@school.edu",
                "9876543202",
                "Password123!",
                "Principal"
        ));

        MvcResult login2 = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "user2_" + suffix2 + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        school2Token = objectMapper.readTree(login2.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("School user creates student and lists students with pagination and class filter")
    void shouldCreateAndListStudents() throws Exception {
        StudentRequest request = new StudentRequest(
                "ADM-101",
                "Aarav Sharma",
                "Rajesh Sharma",
                "Pooja Sharma",
                LocalDate.of(2010, 5, 15),
                "MALE",
                "B+",
                "10th",
                "A",
                "12",
                "42 Lake View Apartments",
                "9876543210",
                "9876543219",
                null,
                "ACTIVE"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/school/students")
                        .header("Authorization", "Bearer " + school1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.admissionNumber", is("ADM-101")))
                .andExpect(jsonPath("$.data.studentName", is("Aarav Sharma")))
                .andExpect(jsonPath("$.data.schoolId", is(school1Id.toString())))
                .andReturn();

        String studentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // List students for School 1
        mockMvc.perform(get("/api/v1/school/students")
                        .header("Authorization", "Bearer " + school1Token)
                        .param("page", "0")
                        .param("size", "10")
                        .param("className", "10th")
                        .param("section", "A"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].admissionNumber", is("ADM-101")));

        // Get single student
        mockMvc.perform(get("/api/v1/school/students/" + studentId)
                        .header("Authorization", "Bearer " + school1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.admissionNumber", is("ADM-101")));
    }

    @Test
    @DisplayName("Duplicate admission number within same school returns 409 RESOURCE_CONFLICT")
    void shouldRejectDuplicateAdmissionNumberInSameSchool() throws Exception {
        StudentRequest request1 = new StudentRequest(
                "ADM-999",
                "Priya Patel",
                "Suresh Patel",
                "Meena Patel",
                LocalDate.of(2011, 8, 20),
                "FEMALE",
                "O+",
                "9th",
                "B",
                "05",
                "Address",
                "9876543210",
                null,
                null,
                "ACTIVE"
        );

        mockMvc.perform(post("/api/v1/school/students")
                        .header("Authorization", "Bearer " + school1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Duplicate admission number for School 1
        StudentRequest request2 = new StudentRequest(
                "ADM-999",
                "Ananya Patel",
                "Suresh Patel",
                "Meena Patel",
                LocalDate.of(2012, 1, 10),
                "FEMALE",
                "O+",
                "7th",
                "A",
                "08",
                "Address",
                "9876543210",
                null,
                null,
                "ACTIVE"
        );

        mockMvc.perform(post("/api/v1/school/students")
                        .header("Authorization", "Bearer " + school1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_CONFLICT")));
    }

    @Test
    @DisplayName("Section 6.1 Rule: Admission number is unique PER SCHOOL, so different schools can have identical admission numbers")
    void differentSchoolsCanShareAdmissionNumber() throws Exception {
        StudentRequest request = new StudentRequest(
                "ADM-COMMON-001",
                "Common Student",
                null,
                null,
                null,
                "MALE",
                "A+",
                "8th",
                "C",
                "01",
                null,
                null,
                null,
                null,
                "ACTIVE"
        );

        // School 1 creates ADM-COMMON-001
        mockMvc.perform(post("/api/v1/school/students")
                        .header("Authorization", "Bearer " + school1Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // School 2 also creates ADM-COMMON-001 (Must succeed!)
        mockMvc.perform(post("/api/v1/school/students")
                        .header("Authorization", "Bearer " + school2Token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.admissionNumber", is("ADM-COMMON-001")))
                .andExpect(jsonPath("$.data.schoolId", is(school2Id.toString())));
    }
}
