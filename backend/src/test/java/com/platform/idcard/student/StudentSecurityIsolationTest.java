package com.platform.idcard.student;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.student.dto.StudentRequest;
import com.platform.idcard.student.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StudentSecurityIsolationTest {

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
    private RoleRepository roleRepository;

    private String schoolAToken;
    private UUID schoolAId;
    private String schoolBToken;
    private UUID schoolBId;
    private String agencyToken;
    private String studentAId;

    @BeforeEach
    void setUp() throws Exception {
        if (roleRepository.findByRoleName("SCHOOL").isEmpty()) {
            roleRepository.save(new Role(null, "SCHOOL", "School role"));
        }
        if (roleRepository.findByRoleName("PRINTING_AGENCY").isEmpty()) {
            roleRepository.save(new Role(null, "PRINTING_AGENCY", "Agency role"));
        }

        // Setup School A
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        var sA = schoolService.createSchool(new SchoolRequest(
                "School Alpha " + suffixA,
                "ALPHA-" + suffixA,
                "Address A",
                "City A",
                "State A",
                "111111",
                "alpha_" + suffixA + "@school.edu",
                "9876543201",
                "ACTIVE"
        ));
        schoolAId = sA.id();
        schoolService.createSchoolUser(schoolAId, new CreateOrganizationUserRequest(
                "Principal Alpha",
                "principal_alpha_" + suffixA + "@school.edu",
                "9876543201",
                "Password123!",
                "Principal"
        ));

        MvcResult loginA = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "principal_alpha_" + suffixA + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        schoolAToken = objectMapper.readTree(loginA.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Setup School B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        var sB = schoolService.createSchool(new SchoolRequest(
                "School Beta " + suffixB,
                "BETA-" + suffixB,
                "Address B",
                "City B",
                "State B",
                "222222",
                "beta_" + suffixB + "@school.edu",
                "9876543202",
                "ACTIVE"
        ));
        schoolBId = sB.id();
        schoolService.createSchoolUser(schoolBId, new CreateOrganizationUserRequest(
                "Principal Beta",
                "principal_beta_" + suffixB + "@school.edu",
                "9876543202",
                "Password123!",
                "Principal"
        ));

        MvcResult loginB = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "principal_beta_" + suffixB + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        schoolBToken = objectMapper.readTree(loginB.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Setup Agency User
        String suffixAgency = UUID.randomUUID().toString().substring(0, 8);
        var agency = agencyService.createAgency(new AgencyRequest(
                "Agency " + suffixAgency,
                "Contact " + suffixAgency,
                "agency_" + suffixAgency + "@print.com",
                "9876543999",
                "Industrial Area",
                5000,
                "Thermal PVC",
                "ACTIVE"
        ));
        agencyService.createAgencyUser(agency.id(), new CreateOrganizationUserRequest(
                "Agency User",
                "user_" + suffixAgency + "@print.com",
                "9876543999",
                "Password123!",
                "Supervisor"
        ));

        MvcResult loginAgency = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "user_" + suffixAgency + "@print.com",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(loginAgency.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // School A creates Student A directly via student service
        var studentResp = studentService.createStudent(schoolAId, new StudentRequest(
                "ADM-SECRET-1",
                "Secret Student Alpha",
                null,
                null,
                null,
                "MALE",
                "O+",
                "10th",
                "A",
                "01",
                null,
                null,
                null,
                null,
                "ACTIVE"
        ));
        studentAId = studentResp.id().toString();
    }

    @Test
    @DisplayName("Non-negotiable Rule 2 & Security Test 19.1: School B cannot read School A's student (returns 404 without disclosure)")
    void schoolBCannotReadSchoolAStudent() throws Exception {
        mockMvc.perform(get("/api/v1/school/students/" + studentAId)
                        .header("Authorization", "Bearer " + schoolBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 2 & Security Test 19.1: School B cannot update School A's student")
    void schoolBCannotUpdateSchoolAStudent() throws Exception {
        StudentRequest updateReq = new StudentRequest(
                "ADM-HACKED",
                "Hacked Student",
                null,
                null,
                null,
                "MALE",
                "O+",
                "10th",
                "A",
                "01",
                null,
                null,
                null,
                null,
                "ACTIVE"
        );

        mockMvc.perform(put("/api/v1/school/students/" + studentAId)
                        .header("Authorization", "Bearer " + schoolBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 2 & Security Test 19.1: School B cannot delete School A's student")
    void schoolBCannotDeleteSchoolAStudent() throws Exception {
        mockMvc.perform(delete("/api/v1/school/students/" + studentAId)
                        .header("Authorization", "Bearer " + schoolBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Agency user cannot access school students endpoint (returns 403 ACCESS_DENIED)")
    void agencyUserCannotAccessStudents() throws Exception {
        mockMvc.perform(get("/api/v1/school/students")
                        .header("Authorization", "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Super Admin can inspect School A's student roster and details")
    void superAdminCanInspectSchoolStudents() throws Exception {
        mockMvc.perform(get("/api/v1/admin/schools/" + schoolAId + "/students/" + studentAId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.admissionNumber", is("ADM-SECRET-1")))
                .andExpect(jsonPath("$.data.studentName", is("Secret Student Alpha")));
    }
}
