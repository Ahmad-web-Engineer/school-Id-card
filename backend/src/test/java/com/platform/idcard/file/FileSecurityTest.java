package com.platform.idcard.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.file.entity.FileEntity;
import com.platform.idcard.file.service.FileStorageService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SchoolService schoolService;

    @Autowired
    private AgencyService agencyService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String schoolAToken;
    private UUID schoolAId;

    private String schoolBToken;
    private UUID schoolBId;
    private UUID fileBId;

    private String agencyToken;
    private String superAdminToken;

    @BeforeEach
    void setUp() throws Exception {
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
        if (!userRepository.existsByEmail("admin@platform.com")) {
            Role adminRole = roleRepository.findByRoleName("SUPER_ADMIN").orElseThrow();
            User admin = new User();
            admin.setFullName("Super Admin");
            admin.setEmail("admin@platform.com");
            admin.setPasswordHash(passwordEncoder.encode("Password123!"));
            admin.setStatus("ACTIVE");
            admin.setRoles(Set.of(adminRole));
            userRepository.save(admin);
        }

        MvcResult adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "admin@platform.com",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        superAdminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 2. Setup School A
        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        var sA = schoolService.createSchool(new SchoolRequest(
                "School Alpha File " + suffixA,
                "FLA-" + suffixA,
                "100 File St",
                "Alpha City",
                "Alpha State",
                "100001",
                "alpha_file_" + suffixA + "@school.edu",
                "9844444401",
                "ACTIVE"
        ));
        schoolAId = sA.id();
        schoolService.createSchoolUser(schoolAId, new CreateOrganizationUserRequest(
                "Principal Alpha File",
                "principal_file_a_" + suffixA + "@school.edu",
                "9844444401",
                "Password123!",
                "Principal"
        ));

        MvcResult loginA = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "principal_file_a_" + suffixA + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        schoolAToken = objectMapper.readTree(loginA.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 3. Setup School B
        String suffixB = UUID.randomUUID().toString().substring(0, 8);
        var sB = schoolService.createSchool(new SchoolRequest(
                "School Beta File " + suffixB,
                "FLB-" + suffixB,
                "200 File Ave",
                "Beta City",
                "Beta State",
                "200002",
                "beta_file_" + suffixB + "@school.edu",
                "9855555502",
                "ACTIVE"
        ));
        schoolBId = sB.id();
        schoolService.createSchoolUser(schoolBId, new CreateOrganizationUserRequest(
                "Principal Beta File",
                "principal_file_b_" + suffixB + "@school.edu",
                "9855555502",
                "Password123!",
                "Principal"
        ));

        MvcResult loginB = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "principal_file_b_" + suffixB + "@school.edu",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        schoolBToken = objectMapper.readTree(loginB.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 4. Setup Printing Agency
        String suffixAgency = UUID.randomUUID().toString().substring(0, 8);
        var agency = agencyService.createAgency(new AgencyRequest(
                "Agency File Sec " + suffixAgency,
                "Contact " + suffixAgency,
                "agency_file_" + suffixAgency + "@print.com",
                "9866666603",
                "Industrial Area",
                5000,
                "PVC",
                "ACTIVE"
        ));
        agencyService.createAgencyUser(agency.id(), new CreateOrganizationUserRequest(
                "Agency File Supervisor",
                "supervisor_file_" + suffixAgency + "@print.com",
                "9866666603",
                "Password123!",
                "Supervisor"
        ));

        MvcResult loginAgency = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(
                                "supervisor_file_" + suffixAgency + "@print.com",
                                "Password123!"
                        ))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(loginAgency.getResponse().getContentAsString())
                .path("data").path("accessToken").asText();

        // 5. Create File belonging to School B
        User userB = userRepository.findByEmail("principal_file_b_" + suffixB + "@school.edu").orElseThrow();
        FileEntity fileEntityB = fileStorageService.storeFile(
                "Confidential School B Document Content".getBytes(StandardCharsets.UTF_8),
                "school_b_document.pdf",
                "application/pdf",
                schoolBId,
                userB.getId()
        );
        fileBId = fileEntityB.getId();
    }

    @Test
    @DisplayName("Non-negotiable Rule 2: School A cannot download School B's file (returns 403 ACCESS_DENIED)")
    void schoolACannotDownloadSchoolBFile() throws Exception {
        mockMvc.perform(get("/api/v1/files/" + fileBId)
                        .header("Authorization", "Bearer " + schoolAToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("Security Requirement: Unauthenticated request receives 401 UNAUTHORIZED on GET /api/v1/files/{fileId}")
    void unauthenticatedRequestCannotDownloadFile() throws Exception {
        mockMvc.perform(get("/api/v1/files/" + fileBId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("UNAUTHORIZED")));
    }

    @Test
    @DisplayName("Security Requirement: SUPER_ADMIN can view and download files across schools")
    void superAdminCanDownloadFileAcrossSchools() throws Exception {
        mockMvc.perform(get("/api/v1/files/" + fileBId)
                        .header("Authorization", "Bearer " + superAdminToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("application/pdf")))
                .andExpect(content().string("Confidential School B Document Content"));
    }

    @Test
    @DisplayName("Non-negotiable Rule 1: Printing Agency cannot download private school files (returns 403 ACCESS_DENIED)")
    void printingAgencyCannotDownloadPrivateSchoolFile() throws Exception {
        mockMvc.perform(get("/api/v1/files/" + fileBId)
                        .header("Authorization", "Bearer " + agencyToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @DisplayName("School B can successfully download its own file")
    void schoolBCanDownloadOwnFile() throws Exception {
        mockMvc.perform(get("/api/v1/files/" + fileBId)
                        .header("Authorization", "Bearer " + schoolBToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", is("application/pdf")))
                .andExpect(content().string("Confidential School B Document Content"));
    }

    @Test
    @DisplayName("Non-existent file returns 404 RESOURCE_NOT_FOUND")
    void nonExistentFileReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/files/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + schoolBToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Security Requirement 4: File storage service prevents directory traversal path resolution")
    void storageServicePreventsDirectoryTraversal() {
        ApiException ex = assertThrows(ApiException.class, () ->
                fileStorageService.resolveSafePath("../../windows/system32/cmd.exe")
        );
        assertEquals(ErrorCode.INVALID_ACTION, ex.getErrorCode());
    }

    @Test
    @DisplayName("Security Requirement 4: File storage service detects and blocks Zip-Slip entry paths")
    void storageServiceBlocksZipSlipEntryPath() {
        ApiException ex = assertThrows(ApiException.class, () ->
                fileStorageService.validateZipEntryPath("../../etc/shadow", fileStorageService.getStorageDirectory())
        );
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
    }
}
