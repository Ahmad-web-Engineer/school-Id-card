package com.platform.idcard.settings;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.settings.dto.UpdatePlatformSettingsRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Admin can fetch platform settings and printing charges")
    void shouldFetchPlatformSettings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.pricingPvcCard").exists())
                .andExpect(jsonPath("$.data.pricingRfidCard").exists())
                .andExpect(jsonPath("$.data.taxRatePercent").exists())
                .andExpect(jsonPath("$.data.platformCurrency", is("INR")));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Admin can update platform printing charges and commercial parameters")
    void shouldUpdatePlatformSettings() throws Exception {
        UpdatePlatformSettingsRequest request = new UpdatePlatformSettingsRequest(
                new BigDecimal("55.00"),
                new BigDecimal("110.00"),
                new BigDecimal("22.00"),
                new BigDecimal("15.00"),
                new BigDecimal("25.00"),
                new BigDecimal("30.00"),
                new BigDecimal("18.00"),
                new BigDecimal("200.00"),
                50,
                45,
                20,
                "DIRECT_TO_SCHOOL",
                "INR"
        );

        mockMvc.perform(put("/api/v1/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.pricingPvcCard", is(55.00)))
                .andExpect(jsonPath("$.data.pricingRfidCard", is(110.00)))
                .andExpect(jsonPath("$.data.pricingLanyard", is(22.00)))
                .andExpect(jsonPath("$.data.minOrderQuantity", is(50)))
                .andExpect(jsonPath("$.data.paymentDueDays", is(45)));
    }

    @Test
    @WithMockUser(roles = "SUPER_ADMIN")
    @DisplayName("Reject negative printing charge with validation error")
    void shouldRejectNegativePricing() throws Exception {
        UpdatePlatformSettingsRequest invalidRequest = new UpdatePlatformSettingsRequest(
                new BigDecimal("-10.00"), // Invalid negative price
                new BigDecimal("110.00"),
                new BigDecimal("22.00"),
                new BigDecimal("15.00"),
                new BigDecimal("25.00"),
                new BigDecimal("30.00"),
                new BigDecimal("18.00"),
                new BigDecimal("200.00"),
                50,
                45,
                20,
                "DIRECT_TO_SCHOOL",
                "INR"
        );

        mockMvc.perform(put("/api/v1/admin/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_FAILED")));
    }

    @Test
    @WithMockUser(roles = "SCHOOL")
    @DisplayName("School user calling Admin settings API receives 403 ACCESS_DENIED")
    void schoolUserCannotAccessAdminSettings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }

    @Test
    @WithMockUser(roles = "PRINTING_AGENCY")
    @DisplayName("Agency user calling Admin settings API receives 403 ACCESS_DENIED")
    void agencyUserCannotAccessAdminSettings() throws Exception {
        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("ACCESS_DENIED")));
    }
}
