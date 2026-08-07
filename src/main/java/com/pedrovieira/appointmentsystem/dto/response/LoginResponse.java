package com.pedrovieira.appointmentsystem.dto.response;

/**
 * Login Response DTO.
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {

}
