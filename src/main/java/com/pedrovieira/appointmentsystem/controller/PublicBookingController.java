package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.request.PublicBookingRequest;
import com.pedrovieira.appointmentsystem.exception.PublicBookingException;
import com.pedrovieira.appointmentsystem.service.PublicBookingService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/** Accepts public booking requests and verifies their email addresses. */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.verification.enabled", havingValue = "true")
@RequestMapping("/public/availability/{linkId}/requests")
public class PublicBookingController {

  private static final MediaType HTML_UTF8 =
      new MediaType("text", "html", StandardCharsets.UTF_8);

  private final PublicBookingService bookingService;

  /** Requests an email verification link for the selected slot. */
  @PostMapping
  public ResponseEntity<Void> request(
      @PathVariable UUID linkId, @Valid @RequestBody PublicBookingRequest request
  ) {
    bookingService.request(linkId, request);
    return ResponseEntity.accepted().build();
  }

  /** Presents an explicit confirmation step before consuming a token. */
  @GetMapping(value = "/verify", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> verificationForm(
      @PathVariable UUID linkId, @RequestParam UUID token
  ) {
    try {
      bookingService.validateVerificationLink(linkId, token);
    } catch (PublicBookingException exception) {
      return verificationResult(exception.getStatus(), exception.getMessage());
    }

    String html = """
        <!doctype html><html lang="pt-BR"><meta charset="utf-8">
        <title>Verificar solicitação de agendamento</title>
        <h1>Verifique seu e-mail</h1>
        <p>Confirme abaixo para solicitar o horário escolhido.</p>
        <form method="post" action="verify">
          <input type="hidden" name="token" value="%s">
          <button type="submit">Verificar e solicitar horário</button>
        </form>
        </html>
        """.formatted(token);
    return ResponseEntity.ok().contentType(HTML_UTF8)
        .cacheControl(CacheControl.noStore()).body(html);
  }

  /** Displays the result when a visitor submits the verification form. */
  @PostMapping(value = "/verify", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
      produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> verifyForm(@PathVariable UUID linkId, @RequestParam UUID token) {
    try {
      bookingService.verify(linkId, token);
      return verificationResult(HttpStatus.CREATED,
          "Solicitação enviada. Agora aguarde a resposta do profissional.");
    } catch (PublicBookingException exception) {
      return verificationResult(exception.getStatus(), exception.getMessage());
    }
  }

  private ResponseEntity<String> verificationResult(HttpStatus status, String message) {
    return ResponseEntity.status(status).contentType(HTML_UTF8)
        .cacheControl(CacheControl.noStore()).body("""
        <!doctype html><html lang="pt-BR"><meta charset="utf-8">
        <title>Solicitação de agendamento</title>
        <h1>Solicitação de agendamento</h1>
        <p>%s</p>
        </html>
        """.formatted(HtmlUtils.htmlEscape(message, StandardCharsets.UTF_8.name())));
  }

  /** Consumes the token and creates a pending appointment if the slot is still available. */
  @PostMapping(value = "/verify", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<BookingHold> verify(@PathVariable UUID linkId, @RequestParam UUID token) {
    Instant expiresAt = bookingService.verify(linkId, token);
    return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
        .body(new BookingHold(expiresAt));
  }

  /** The deadline for reviewing the newly created booking request. */
  public record BookingHold(Instant expiresAt) {
  }
}
