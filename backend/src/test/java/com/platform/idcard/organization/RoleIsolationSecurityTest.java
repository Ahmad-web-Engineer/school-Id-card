package com.platform.idcard.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
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

import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoleIsolationSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private RoleRepository roleRepository;

    private String schoolUserToken;
    private UUID registeredSchoolId;
    private String agencyUserToken;
    private UUID registeredAgencyId;

    @BeforeEach
    void setUp() throws Exception {
        if (roleRepository.findByRoleName("SCHOOL").isEmpty()) {
            roleRepository.save(new Role(null, "SCHOOL", "School role"));
        }
        if (roleRepository.findByRoleName("PRINTING_AGENCY").isEmpty()) {
            roleRepository.save(new Role(null, "PRINTING_AGENCY", "Agency role"));
        }

        // Setup a test school and user
        String uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
        SchoolRequest schoolReq = new SchoolRequest(
                "Isolation High " + uniqueSuffix,
                "ISO-" + uniqueSuffix,
                "Address",
                "City",
                "State",
                "123456",
                "iso_school_" + uniqueSuffix + "@test.com",
                "9999999999",
                "ACTIVE"
        );
        var schoolResp = schoolService.createSchool(schoolReq);
        registeredSchoolId = schoolResp.id();

        schoolService.createSchoolUser(registeredSchoolId, new CreateOrganizationUserRequest(
                "School Officer",
                "officer_" + uniqueSuffix + "@test.com",
                "9999999998",
                "Password123!",
                "Principal"
        ));

        // Login as school user
        MvcResult schoolLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "officer_" + uniqueSuffix + "@test.com",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        schoolUserToken = objectMapper.readTree(schoolLogin.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // Setup a test agency and user
        AgencyRequest agencyReq = new AgencyRequest(
                "Isolation Agency " + uniqueSuffix,
                "Operator",
                "iso_agency_" + uniqueSuffix + "@test.com",
                "8888888888",
                "Industrial Area",
                5000,
                "PVC",
                "ACTIVE"
        );
        var agencyResp = agencyService.createAgency(agencyReq);
        registeredAgencyId = agencyResp.id();

        agencyService.createAgencyUser(registeredAgencyId, new CreateOrganizationUserRequest(
                "Agency Supervisor",
                "supervisor_" + uniqueSuffix + "@test.com",
                "8888888887",
                "Password123!",
                "Supervisor"
        ));

        // Login as agency user
        MvcResult agencyLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "supervisor_" + uniqueSuffix + "@test.com",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        agencyUserToken = objectMapper.readTree(agencyLogin.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: School user calling Agency profile receives 403 ACCESS_DENIED")
    void schoolUserCannotAccessAgencyProfile() throws Exception {
        mockMvc.perform(get("/api/v1/agency/profile")
                        .header("Authorization", "Bearer " + schoolUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Agency user calling School profile receives 403 ACCESS_DENIED")
    void agencyUserCannotAccessSchoolProfile() throws Exception {
        mockMvc.perform(get("/api/v1/school/profile")
                        .header("Authorization", "Bearer " + agencyUserToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Non-negotiable Rule 2: School user receives strictly their own school profile derived from token")
    void schoolUserReceivesOwnProfileFromToken() throws Exception {
        mockMvc.perform(get("/api/v1/school/profile")
                        .header("Authorization", "Bearer " + schoolUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(registeredSchoolId.toString())));
    }

    @Test
    @DisplayName("Non-negotiable Rule 2: Agency user receives strictly their own agency profile derived from token")
    void agencyUserReceivesOwnProfileFromToken() throws Exception {
        mockMvc.perform(get("/api/v1/agency/profile")
                        .header("Authorization", "Bearer " + agencyUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.id", is(registeredAgencyId.toString())));
    }
}
