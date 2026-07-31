package com.pedrovieira.appointmentsystem.dto.response;

/**
 * Standard user response DTO.
 */
public record UserResponse(
    Long id,
    String fullName,
    String email
) {

}
