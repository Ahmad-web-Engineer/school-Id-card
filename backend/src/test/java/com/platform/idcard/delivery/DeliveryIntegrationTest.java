package com.platform.idcard.delivery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.agencyjob.dto.AgencyAssignmentItemRequest;
import com.platform.idcard.agencyjob.dto.AssignOrderRequest;
import com.platform.idcard.agencyjob.dto.JobItemAllocationRequest;
import com.platform.idcard.agencyjob.service.AgencyJobService;
import com.platform.idcard.auth.dto.LoginRequest;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.delivery.dto.AdminSecondaryDispatchRequest;
import com.platform.idcard.delivery.dto.CloseDeliveryRequest;
import com.platform.idcard.delivery.dto.ConfirmDeliveryRequest;
import com.platform.idcard.delivery.dto.CreateDispatchPayload;
import com.platform.idcard.delivery.dto.HubReceiveRequest;
import com.platform.idcard.order.dto.CreateOrderRequest;
import com.platform.idcard.order.dto.GenerateQuotationRequest;
import com.platform.idcard.order.dto.QuotationItemRequest;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.service.AgencyService;
import com.platform.idcard.organization.service.SchoolService;
import com.platform.idcard.student.dto.StudentRequest;
import com.platform.idcard.student.service.StudentService;
import com.platform.idcard.template.dto.AssignTemplateRequest;
import com.platform.idcard.template.dto.CreateCardTemplateRequest;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeliveryIntegrationTest {

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
    private AgencyJobService agencyJobService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String adminToken;
    private UUID adminUserId;
    private String schoolToken;
    private String agencyToken;

    private UUID schoolId;
    private UUID agencyId;
    private UUID templateVerId;

    @BeforeEach
    void setUp() throws Exception {
        Role adminRole = roleRepository.findByRoleName("SUPER_ADMIN")
                .orElseGet(() -> roleRepository.save(new Role(null, "SUPER_ADMIN", "Admin")));
        Role schoolRole = roleRepository.findByRoleName("SCHOOL")
                .orElseGet(() -> roleRepository.save(new Role(null, "SCHOOL", "School")));
        Role agencyRole = roleRepository.findByRoleName("PRINTING_AGENCY")
                .orElseGet(() -> roleRepository.save(new Role(null, "PRINTING_AGENCY", "Agency")));

        User adminUser = userRepository.findByEmail("admin@platform.com").orElse(null);
        if (adminUser == null) {
            adminUser = new User();
            adminUser.setFullName("Super Admin");
            adminUser.setEmail("admin@platform.com");
            adminUser.setPasswordHash(passwordEncoder.encode("Password123!"));
            adminUser.setStatus("ACTIVE");
            adminUser.setRoles(Set.of(adminRole));
            adminUser = userRepository.save(adminUser);
        }
        adminUserId = adminUser.getId();

        MvcResult adminLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("admin@platform.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(adminLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        String suffix = UUID.randomUUID().toString().substring(0, 8);
        var sDto = schoolService.createSchool(new SchoolRequest(
                "School Del IT " + suffix, "SCH-DIT-" + suffix, "10 Campus Way",
                "Metropolis", "State X", "500001", "del_schit_" + suffix + "@test.edu",
                "9876543101", "ACTIVE"
        ));
        schoolId = sDto.id();

        schoolService.createSchoolUser(schoolId, new CreateOrganizationUserRequest(
                "School IT Manager", "del_usrit_" + suffix + "@test.edu", "9876543101", "Password123!", "Admin"
        ));

        MvcResult sLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("del_usrit_" + suffix + "@test.edu", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        schoolToken = objectMapper.readTree(sLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        String suffixA = UUID.randomUUID().toString().substring(0, 8);
        var agDto = agencyService.createAgency(new AgencyRequest(
                "Agency Del IT " + suffixA, "Contact IT", "del_agit_" + suffixA + "@print.com",
                "9876543102", "Industrial Area IT", 50000, "500002", "ACTIVE"
        ));
        agencyId = agDto.id();

        agencyService.createAgencyUser(agencyId, new CreateOrganizationUserRequest(
                "Agency IT Manager", "del_aguit_" + suffixA + "@print.com", "9876543102", "Password123!", "Manager"
        ));

        MvcResult agLogin = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest("del_aguit_" + suffixA + "@print.com", "Password123!"))))
                .andExpect(status().isOk())
                .andReturn();
        agencyToken = objectMapper.readTree(agLogin.getResponse().getContentAsString()).path("data").path("accessToken").asText();

        String tplName = "Del IT Template " + suffix;
        MvcResult tplRes = mockMvc.perform(post("/api/v1/admin/templates")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCardTemplateRequest(
                                tplName, "STUDENT", null,
                                new BigDecimal("85.60"), new BigDecimal("54.00"), new BigDecimal("3.00"),
                                "{\"elements\":[]}", "{\"elements\":[]}"
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        String tplId = objectMapper.readTree(tplRes.getResponse().getContentAsString()).path("data").path("id").asText();
        String v1Id = objectMapper.readTree(tplRes.getResponse().getContentAsString()).path("data").path("versions").get(0).path("id").asText();

        mockMvc.perform(post("/api/v1/admin/templates/" + tplId + "/versions/" + v1Id + "/submit")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        MvcResult appRes = mockMvc.perform(post("/api/v1/admin/templates/" + tplId + "/versions/" + v1Id + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        templateVerId = UUID.fromString(
                objectMapper.readTree(appRes.getResponse().getContentAsString()).path("data").path("id").asText()
        );

        mockMvc.perform(post("/api/v1/admin/templates/" + tplId + "/assign")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignTemplateRequest(schoolId, "2026-2027", true))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("End-to-end delivery lifecycle: Model A (Direct from Agency to School)")
    void modelADirectDeliveryLifecycle() throws Exception {
        String sfx = UUID.randomUUID().toString().substring(0, 4);
        var s1 = studentService.createStudent(schoolId, new StudentRequest(
                "ADM-A1-" + sfx, "Student A1", "Father", "Mother",
                LocalDate.of(2010, 1, 1), "MALE", "A+", "10", "A", "01",
                "Address", "9876543000", null, null, "ACTIVE"
        ));

        MvcResult orderRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                templateVerId, "2026-2027", "DIRECT_TO_SCHOOL",
                                "Main Campus Reception", "Officer Sharma", "9876543101", null,
                                List.of(s1.id()), null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode orderData = objectMapper.readTree(orderRes.getResponse().getContentAsString()).path("data");
        UUID orderId = UUID.fromString(orderData.path("id").asText());
        UUID orderItemId = UUID.fromString(orderData.path("items").get(0).path("id").asText());

        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/submit").header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/start-review").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/verify").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateQuotationRequest(
                                BigDecimal.ZERO, new BigDecimal("18.00"), 10, "Quote",
                                List.of(new QuotationItemRequest("PVC Card", 1, new BigDecimal("100.00")))
                        ))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/approve-quotation").header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)).andExpect(status().isOk());

        var assignments = agencyJobService.assignOrderToAgencies(orderId, adminUserId, new AssignOrderRequest(
                List.of(new AgencyAssignmentItemRequest(
                        agencyId, LocalDate.now().plusDays(5), new BigDecimal("40.00"), "Print run",
                        List.of(new JobItemAllocationRequest(orderItemId, 1))
                ))
        ));
        UUID assignmentId = assignments.get(0).id();

        mockMvc.perform(post("/api/v1/agency/jobs/" + assignmentId + "/accept").header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/agency/jobs/" + assignmentId + "/dispatch")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDispatchPayload(
                                "BlueDart", "TRK-A-1001", 1, "1 box dispatched", null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dispatchStatus").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.data.dispatchType").value("AGENCY_TO_SCHOOL"));

        MvcResult orderAfterDispatch = mockMvc.perform(get("/api/v1/school/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)).andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(orderAfterDispatch.getResponse().getContentAsString()).path("data").path("orderStatus").asText()).isEqualTo("DISPATCHED");

        MvcResult deliveryRes = mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/confirm-delivery")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmDeliveryRequest(
                                1, "Principal Sharma", "9876543101", "Received 1 card in good order", null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deliveryStatus").value("CONFIRMED"))
                .andReturn();
        UUID deliveryId = UUID.fromString(objectMapper.readTree(deliveryRes.getResponse().getContentAsString()).path("data").path("id").asText());

        MvcResult orderAfterDelivery = mockMvc.perform(get("/api/v1/school/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)).andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(orderAfterDelivery.getResponse().getContentAsString()).path("data").path("orderStatus").asText()).isEqualTo("DELIVERED");

        mockMvc.perform(post("/api/v1/admin/delivery/records/" + deliveryId + "/close")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CloseDeliveryRequest("Verified and closed"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deliveryStatus").value("CLOSED"));
    }

    @Test
    @DisplayName("End-to-end delivery lifecycle: Model B (Agency -> Admin Hub -> School)")
    void modelBHubDeliveryLifecycle() throws Exception {
        String sfx = UUID.randomUUID().toString().substring(0, 4);
        var s1 = studentService.createStudent(schoolId, new StudentRequest(
                "ADM-B1-" + sfx, "Student B1", "Father", "Mother",
                LocalDate.of(2009, 3, 3), "MALE", "O+", "11", "B", "10",
                "Address", "9876543000", null, null, "ACTIVE"
        ));

        MvcResult orderRes = mockMvc.perform(post("/api/v1/school/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateOrderRequest(
                                templateVerId, "2026-2027", "VIA_ADMIN_HUB",
                                "Main Gate Hub drop", "Security Hub", "9876543101", null,
                                List.of(s1.id()), null
                        ))))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode orderData = objectMapper.readTree(orderRes.getResponse().getContentAsString()).path("data");
        UUID orderId = UUID.fromString(orderData.path("id").asText());
        UUID orderItemId = UUID.fromString(orderData.path("items").get(0).path("id").asText());

        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/submit").header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/start-review").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/verify").header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/quotation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new GenerateQuotationRequest(
                                BigDecimal.ZERO, new BigDecimal("18.00"), 10, "Quote",
                                List.of(new QuotationItemRequest("PVC Card", 1, new BigDecimal("100.00")))
                        ))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/approve-quotation").header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)).andExpect(status().isOk());

        var assignments = agencyJobService.assignOrderToAgencies(orderId, adminUserId, new AssignOrderRequest(
                List.of(new AgencyAssignmentItemRequest(
                        agencyId, LocalDate.now().plusDays(5), new BigDecimal("40.00"), "Hub dispatch",
                        List.of(new JobItemAllocationRequest(orderItemId, 1))
                ))
        ));
        UUID assignmentId = assignments.get(0).id();

        mockMvc.perform(post("/api/v1/agency/jobs/" + assignmentId + "/accept").header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken)).andExpect(status().isOk());

        MvcResult agencyDispatchRes = mockMvc.perform(post("/api/v1/agency/jobs/" + assignmentId + "/dispatch")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agencyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateDispatchPayload(
                                "DTDC Cargo", "DTDC-HUB-555", 1, "Dispatched to central admin hub", null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dispatchStatus").value("IN_TRANSIT"))
                .andExpect(jsonPath("$.data.dispatchType").value("AGENCY_TO_HUB"))
                .andReturn();
        UUID dispatchId = UUID.fromString(objectMapper.readTree(agencyDispatchRes.getResponse().getContentAsString()).path("data").path("id").asText());

        mockMvc.perform(post("/api/v1/admin/delivery/dispatches/" + dispatchId + "/receive-hub")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HubReceiveRequest(
                                1, "GOOD", "Received and QC inspected at hub"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dispatchStatus").value("DELIVERED_TO_HUB"));

        mockMvc.perform(post("/api/v1/admin/delivery/orders/" + orderId + "/dispatch-to-school")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AdminSecondaryDispatchRequest(
                                "Delhivery", "DELH-HUB-777", 1, "Secondary dispatch to school"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.dispatchType").value("HUB_TO_SCHOOL"));

        mockMvc.perform(post("/api/v1/school/orders/" + orderId + "/confirm-delivery")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ConfirmDeliveryRequest(
                                1, "Vice Principal Rao", "9876543101", "Received from admin courier", null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.deliveryStatus").value("CONFIRMED"));

        MvcResult finalOrderFetch = mockMvc.perform(get("/api/v1/school/orders/" + orderId).header(HttpHeaders.AUTHORIZATION, "Bearer " + schoolToken)).andExpect(status().isOk()).andReturn();
        assertThat(objectMapper.readTree(finalOrderFetch.getResponse().getContentAsString()).path("data").path("orderStatus").asText()).isEqualTo("DELIVERED");
    }
}
