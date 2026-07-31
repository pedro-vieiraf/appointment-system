package com.pedrovieira.appointmentsystem.exception;

import com.pedrovieira.appointmentsystem.exception.user.EmailAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global Exception Handler to catch all exceptions in Controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * ExceptionHandler for EmailAlreadyExists.
   */
  @ExceptionHandler(EmailAlreadyExistsException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleEmailAlreadyExists(
      EmailAlreadyExistsException exception,
      HttpServletRequest request
  ) {
    return new ErrorResponse(
        LocalDateTime.now(),
        HttpStatus.CONFLICT.value(),
        HttpStatus.CONFLICT.getReasonPhrase(),
        exception.getMessage(),
        request.getRequestURI()
    );
  }

  /**
   * ExceptionHandler for MethodArgumentNotValid.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleValidation(
      MethodArgumentNotValidException exception,
      HttpServletRequest request
  ) {
    String message = exception
        .getBindingResult()
        .getFieldError()
        .getDefaultMessage();

    return new ErrorResponse(
        LocalDateTime.now(),
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        message,
        request.getRequestURI()
    );
  }

  /**
   * ExceptionHandler for HttpMessageNotReadable.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleMalformedJson(
      HttpMessageNotReadableException exception,
      HttpServletRequest request
  ) {
    return new ErrorResponse(
        LocalDateTime.now(),
        HttpStatus.BAD_REQUEST.value(),
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        "Malformed JSON request.",
        request.getRequestURI()
    );
  }
}