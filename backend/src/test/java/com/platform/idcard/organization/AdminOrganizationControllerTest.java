package com.platform.idcard.organization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminOrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        if (roleRepository.findByRoleName("SCHOOL").isEmpty()) {
            roleRepository.save(new Role(null, "SCHOOL", "School role"));
        }
        if (roleRepository.findByRoleName("PRINTING_AGENCY").isEmpty()) {
            roleRepository.save(new Role(null, "PRINTING_AGENCY", "Agency role"));
        }
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Admin can register school, retrieve it in paginated list, and create a school user")
    void shouldRegisterSchoolAndCreateUser() throws Exception {
        SchoolRequest request = new SchoolRequest(
                "St. Xavier School",
                "STX-001",
                "12 Church Street",
                "Mumbai",
                "Maharashtra",
                "400001",
                "info@stxaviers.edu",
                "9876543200",
                "ACTIVE"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/admin/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.schoolCode", is("STX-001")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn();

        String schoolId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Check paginated list
        mockMvc.perform(get("/api/v1/admin/schools")
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "Xavier"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.pagination.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].schoolCode", is("STX-001")));

        // Create school user
        CreateOrganizationUserRequest userReq = new CreateOrganizationUserRequest(
                "Father Thomas",
                "principal@stxaviers.edu",
                "9876543210",
                "Password123!",
                "Principal"
        );

        mockMvc.perform(post("/api/v1/admin/schools/" + schoolId + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("principal@stxaviers.edu")))
                .andExpect(jsonPath("$.data.role", is("SCHOOL")))
                .andExpect(jsonPath("$.data.schoolId", is(schoolId)));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Registering school with duplicate code returns 409 RESOURCE_CONFLICT")
    void shouldRejectDuplicateSchoolCode() throws Exception {
        SchoolRequest request1 = new SchoolRequest(
                "Delhi Public School",
                "DPS-001",
                "Sector 4",
                "Delhi",
                "Delhi",
                "110001",
                "contact@dps001.edu",
                "9876543201",
                "ACTIVE"
        );

        mockMvc.perform(post("/api/v1/admin/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        SchoolRequest duplicateRequest = new SchoolRequest(
                "Delhi Public School Branch 2",
                "DPS-001", // duplicate code
                "Sector 5",
                "Delhi",
                "Delhi",
                "110001",
                "contact2@dps001.edu",
                "9876543202",
                "ACTIVE"
        );

        mockMvc.perform(post("/api/v1/admin/schools")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_CONFLICT")));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Admin can register agency, retrieve it in paginated list, and create an agency user")
    void shouldRegisterAgencyAndCreateUser() throws Exception {
        AgencyRequest request = new AgencyRequest(
                "Speedy Prints Ltd",
                "Vikram Sharma",
                "contact@speedyprints.com",
                "9876543333",
                "45 Industrial Estate",
                10000,
                "Thermal PVC, Laser Engraving",
                "ACTIVE"
        );

        MvcResult result = mockMvc.perform(post("/api/v1/admin/agencies")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.agencyName", is("Speedy Prints Ltd")))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andReturn();

        String agencyId = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("id").asText();

        // Check paginated list
        mockMvc.perform(get("/api/v1/admin/agencies")
                        .param("page", "0")
                        .param("size", "10")
                        .param("search", "Speedy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.pagination.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data[0].agencyName", is("Speedy Prints Ltd")));

        // Create agency user
        CreateOrganizationUserRequest userReq = new CreateOrganizationUserRequest(
                "Rajesh Kumar",
                "rajesh@speedyprints.com",
                "9876543344",
                "Password123!",
                "Production Manager"
        );

        mockMvc.perform(post("/api/v1/admin/agencies/" + agencyId + "/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("rajesh@speedyprints.com")))
                .andExpect(jsonPath("$.data.role", is("PRINTING_AGENCY")))
                .andExpect(jsonPath("$.data.agencyId", is(agencyId)));
    }

    @Test
    @WithMockUser(roles = "SCHOOL")
    @DisplayName("Non-negotiable rule 1 & 2: School user calling Admin organization API receives 403 ACCESS_DENIED")
    void schoolUserCannotAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/admin/agencies"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }
}
