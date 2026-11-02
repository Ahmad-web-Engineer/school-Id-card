package com.platform.idcard.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserSummaryResponse user
) {
    public static LoginResponse of(String accessToken, String refreshToken, long expiresIn, UserSummaryResponse user) {
        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}
