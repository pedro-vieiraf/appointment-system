package com.pedrovieira.appointmentsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;

/** A visitor's request for one of the published slots. */
public record PublicBookingRequest(
    @NotBlank(message = "Informe seu nome.")
    @Size(max = 100, message = "Use até 100 caracteres no nome.")
    String name,
    @NotBlank(message = "Informe seu e-mail.")
    @Email(message = "Informe um e-mail válido.")
    @Size(max = 254, message = "Use até 254 caracteres no e-mail.")
    String email,
    @NotNull(message = "Escolha uma data na agenda.") LocalDate date,
    @NotNull(message = "Escolha um horário na agenda.") Instant start
) {
}
