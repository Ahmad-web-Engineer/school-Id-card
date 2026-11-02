package com.platform.idcard.settings.service;

import com.platform.idcard.settings.dto.PlatformSettingsResponse;
import com.platform.idcard.settings.dto.UpdatePlatformSettingsRequest;
import com.platform.idcard.settings.entity.PlatformSetting;
import com.platform.idcard.settings.repository.PlatformSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PlatformSettingService {

    public static final String KEY_PRICING_PVC_CARD = "PRICING_PVC_CARD";
    public static final String KEY_PRICING_RFID_CARD = "PRICING_RFID_CARD";
    public static final String KEY_PRICING_LANYARD = "PRICING_LANYARD";
    public static final String KEY_PRICING_CARD_HOLDER = "PRICING_CARD_HOLDER";
    public static final String KEY_PRICING_RUSH_FEE = "PRICING_RUSH_FEE";
    public static final String KEY_AGENCY_COST_PER_CARD = "AGENCY_COST_PER_CARD";
    public static final String KEY_TAX_RATE_PERCENT = "TAX_RATE_PERCENT";
    public static final String KEY_FLAT_SHIPPING_FEE = "FLAT_SHIPPING_FEE";
    public static final String KEY_MIN_ORDER_QUANTITY = "MIN_ORDER_QUANTITY";
    public static final String KEY_PAYMENT_DUE_DAYS = "PAYMENT_DUE_DAYS";
    public static final String KEY_QUOTATION_VALIDITY_DAYS = "QUOTATION_VALIDITY_DAYS";
    public static final String KEY_DEFAULT_DELIVERY_MODEL = "DEFAULT_DELIVERY_MODEL";
    public static final String KEY_PLATFORM_CURRENCY = "PLATFORM_CURRENCY";

    private final PlatformSettingRepository settingRepository;

    public PlatformSettingService(PlatformSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional(readOnly = true)
    public PlatformSettingsResponse getSettings() {
        Map<String, PlatformSetting> map = settingRepository.findAll().stream()
                .collect(Collectors.toMap(PlatformSetting::getKey, s -> s));

        Instant latestUpdate = map.values().stream()
                .map(PlatformSetting::getUpdatedAt)
                .max(Instant::compareTo)
                .orElse(Instant.now());

        return new PlatformSettingsResponse(
                getDecimalValue(map, KEY_PRICING_PVC_CARD, "45.00"),
                getDecimalValue(map, KEY_PRICING_RFID_CARD, "95.00"),
                getDecimalValue(map, KEY_PRICING_LANYARD, "18.00"),
                getDecimalValue(map, KEY_PRICING_CARD_HOLDER, "12.00"),
                getDecimalValue(map, KEY_PRICING_RUSH_FEE, "20.00"),
                getDecimalValue(map, KEY_AGENCY_COST_PER_CARD, "25.00"),
                getDecimalValue(map, KEY_TAX_RATE_PERCENT, "18.00"),
                getDecimalValue(map, KEY_FLAT_SHIPPING_FEE, "150.00"),
                getIntValue(map, KEY_MIN_ORDER_QUANTITY, 25),
                getIntValue(map, KEY_PAYMENT_DUE_DAYS, 30),
                getIntValue(map, KEY_QUOTATION_VALIDITY_DAYS, 15),
                getStringValue(map, KEY_DEFAULT_DELIVERY_MODEL, "DIRECT_TO_SCHOOL"),
                getStringValue(map, KEY_PLATFORM_CURRENCY, "INR"),
                latestUpdate
        );
    }

    @Transactional
    public PlatformSettingsResponse updateSettings(UpdatePlatformSettingsRequest request) {
        saveOrUpdate(KEY_PRICING_PVC_CARD, formatDecimal(request.pricingPvcCard()), "PRICING", "Base selling price per PVC ID card in INR");
        saveOrUpdate(KEY_PRICING_RFID_CARD, formatDecimal(request.pricingRfidCard()), "PRICING", "Base selling price per RFID smart card in INR");
        saveOrUpdate(KEY_PRICING_LANYARD, formatDecimal(request.pricingLanyard()), "PRICING", "Selling price per custom lanyard ribbon in INR");
        saveOrUpdate(KEY_PRICING_CARD_HOLDER, formatDecimal(request.pricingCardHolder()), "PRICING", "Selling price per protective card pouch / holder in INR");
        saveOrUpdate(KEY_PRICING_RUSH_FEE, formatDecimal(request.pricingRushFee()), "PRICING", "Priority / rush production surcharge per card in INR");
        saveOrUpdate(KEY_AGENCY_COST_PER_CARD, formatDecimal(request.agencyCostPerCard()), "AGENCY", "Standard manufacturing cost paid to printing agency partner per card in INR");
        saveOrUpdate(KEY_TAX_RATE_PERCENT, formatDecimal(request.taxRatePercent()), "COMMERCIAL", "Default GST tax rate percentage applied to school orders");
        saveOrUpdate(KEY_FLAT_SHIPPING_FEE, formatDecimal(request.flatShippingFee()), "COMMERCIAL", "Flat courier & delivery fee per shipment in INR");
        saveOrUpdate(KEY_MIN_ORDER_QUANTITY, String.valueOf(request.minOrderQuantity()), "COMMERCIAL", "Minimum order quantity threshold for custom ID card production");
        saveOrUpdate(KEY_PAYMENT_DUE_DAYS, String.valueOf(request.paymentDueDays()), "COMMERCIAL", "Default invoice payment terms in days from invoice generation");
        saveOrUpdate(KEY_QUOTATION_VALIDITY_DAYS, String.valueOf(request.quotationValidityDays()), "COMMERCIAL", "Period for which an issued quotation remains valid in days");
        saveOrUpdate(KEY_DEFAULT_DELIVERY_MODEL, request.defaultDeliveryModel().trim(), "OPERATIONAL", "Default shipping route");
        saveOrUpdate(KEY_PLATFORM_CURRENCY, request.platformCurrency().trim().toUpperCase(), "OPERATIONAL", "Base platform currency code");

        return getSettings();
    }

    private void saveOrUpdate(String key, String value, String category, String description) {
        PlatformSetting setting = settingRepository.findById(key)
                .orElse(new PlatformSetting(key, value, category, description));
        setting.setValue(value);
        setting.setCategory(category);
        setting.setDescription(description);
        setting.setUpdatedAt(Instant.now());
        settingRepository.save(setting);
    }

    private BigDecimal getDecimalValue(Map<String, PlatformSetting> map, String key, String defaultValue) {
        PlatformSetting s = map.get(key);
        if (s == null || s.getValue() == null || s.getValue().isBlank()) {
            return new BigDecimal(defaultValue).setScale(2, RoundingMode.HALF_UP);
        }
        try {
            return new BigDecimal(s.getValue().trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return new BigDecimal(defaultValue).setScale(2, RoundingMode.HALF_UP);
        }
    }

    private Integer getIntValue(Map<String, PlatformSetting> map, String key, int defaultValue) {
        PlatformSetting s = map.get(key);
        if (s == null || s.getValue() == null || s.getValue().isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(s.getValue().trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getStringValue(Map<String, PlatformSetting> map, String key, String defaultValue) {
        PlatformSetting s = map.get(key);
        if (s == null || s.getValue() == null || s.getValue().isBlank()) {
            return defaultValue;
        }
        return s.getValue().trim();
    }

    private String formatDecimal(BigDecimal val) {
        return val.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
