package com.pedrovieira.appointmentsystem.dto.response;

/**
 * Register Response DTO.
 */
public record RegisterResponse(
    Long id,
    String fullName,
    String email
) {

}
