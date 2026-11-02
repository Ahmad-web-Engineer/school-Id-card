package com.platform.idcard.dashboard;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.student.dto.StudentRequest;
import com.platform.idcard.student.service.StudentService;
import com.platform.idcard.template.dto.CreateCardTemplateRequest;
import com.platform.idcard.template.service.CardTemplateService;
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
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private StudentService studentService;

    @Autowired
    private CardTemplateService cardTemplateService;

    private String adminToken;
    private String schoolToken;
    private String agencyToken;

    @BeforeEach
    void setUp() throws Exception {
        Role adminRole = roleRepository.findByRoleName("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "SUPER_ADMIN", "Administrator")));
        if (roleRepository.findByRoleName("SCHOOL").isEmpty()) {
            roleRepository.save(new Role(null, "SCHOOL", "School"));
        }
        if (roleRepository.findByRoleName("PRINTING_AGENCY").isEmpty()) {
            roleRepository.save(new Role(null, "PRINTING_AGENCY", "Agency"));
        }

        User adminUser = userRepository.findByEmail("admin@platform.com").orElse(null);
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setFullName("Super Admin");
            adminUser.setEmail("admin@platform.com");
            adminUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            adminUser.setStatus("ACTIVE");
            adminUser.setRoles(Set.of(adminRole));
            userRepository.save(adminUser);
        } else {
            adminUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            userRepository.save(adminUser);
        }

        MvcResult adminRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@platform.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Ensure School with students & template
        String schoolSuffix = UUID.randomUUID().toString().substring(0, 8);
        var school = schoolService.createSchool(new SchoolRequest(
                "Greenwood Test School " + schoolSuffix, "GWIS-" + schoolSuffix, "14 Heritage Blvd",
                "Bengaluru", "Karnataka", "560038", "dash_sch_" + schoolSuffix + "@greenwood.edu",
                "9876500001", "ACTIVE"
        ));
        schoolService.createSchoolUser(school.id(), new CreateOrganizationUserRequest(
                "Dr. Rajesh Verma", "dash_user_" + schoolSuffix + "@greenwood.edu", "9876500001", "Password123!", "Principal"
        ));

        for (int i = 1; i <= 3; i++) {
            studentService.createStudent(school.id(), new StudentRequest(
                    "ADM-" + schoolSuffix + "-" + i, "Student " + i, "Father", "Mother",
                    LocalDate.of(2012, 1, i), "MALE", "O+", "10", "A", String.format("%02d", i),
                    "City", "9876543000", null, null, "ACTIVE"
            ));
        }

        mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCardTemplateRequest(
                                "School Card Template " + schoolSuffix, "STUDENT", school.id(),
                                new BigDecimal("85.60"), new BigDecimal("54.00"), new BigDecimal("3.00"),
                                "{}", "{}", true
                        ))))
                .andExpect(status().isCreated());

        MvcResult schoolRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("dash_user_" + schoolSuffix + "@greenwood.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolToken = objectMapper.readTree(schoolRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        // Agency
        String agencySuffix = UUID.randomUUID().toString().substring(0, 8);
        var agency = agencyService.createAgency(new AgencyRequest(
                "Apex Print " + agencySuffix, "Suresh Patel", "dash_ag_" + agencySuffix + "@apexprints.com",
                "9876500002", "Peenya Industrial Area", 5000, "560058", "ACTIVE"
        ));
        agencyService.createAgencyUser(agency.id(), new CreateOrganizationUserRequest(
                "Suresh Patel", "dash_ag_user_" + agencySuffix + "@apexprints.com", "9876500002", "Password123!", "Manager"
        ));

        MvcResult agencyRes = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("dash_ag_user_" + agencySuffix + "@apexprints.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(agencyRes.getResponse().getContentAsString()).path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Admin Dashboard: Super Admin gets real-time platform KPIs, recent orders and revenue")
    void testAdminDashboardStats() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.totalSchools", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalAgencies", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalStudents", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.totalOrders", notNullValue()))
                .andExpect(jsonPath("$.data.totalRevenue", notNullValue()))
                .andExpect(jsonPath("$.data.recentOrders", notNullValue()))
                .andExpect(jsonPath("$.data.statusDistribution", notNullValue()));
    }

    @Test
    @DisplayName("School Dashboard: Greenwood School gets enrolled student count and approved templates")
    void testSchoolDashboardStats() throws Exception {
        mockMvc.perform(get("/api/v1/school/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.enrolledStudents", greaterThanOrEqualTo(3)))
                .andExpect(jsonPath("$.data.approvedTemplates", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.totalOrders", notNullValue()))
                .andExpect(jsonPath("$.data.recentOrders", notNullValue()));
    }

    @Test
    @DisplayName("Agency Dashboard: Apex Printers gets real capacity, job queues and earnings")
    void testAgencyDashboardStats() throws Exception {
        mockMvc.perform(get("/api/v1/agency/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.productionCapacity", is(5000)))
                .andExpect(jsonPath("$.data.totalJobs", notNullValue()))
                .andExpect(jsonPath("$.data.totalEarnings", notNullValue()));
    }

    @Test
    @DisplayName("Role Isolation: School cannot call Admin dashboard, Agency cannot call School dashboard")
    void testDashboardRoleIsolation() throws Exception {
        // School calling admin dashboard -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isForbidden());

        // Agency calling admin dashboard -> 403 Forbidden
        mockMvc.perform(get("/api/v1/admin/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken))
                .andExpect(status().isForbidden());

        // School calling agency dashboard -> 403 Forbidden
        mockMvc.perform(get("/api/v1/agency/dashboard/stats")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken))
                .andExpect(status().isForbidden());
    }
}
