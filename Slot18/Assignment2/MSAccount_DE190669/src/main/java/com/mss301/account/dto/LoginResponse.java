package com.mss301.account.dto;

public record LoginResponse(String accessToken, String refreshToken, String tokenType,
                            Integer role, String username) {
}
