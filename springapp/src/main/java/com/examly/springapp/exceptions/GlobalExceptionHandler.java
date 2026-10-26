package com.examly.springapp.exceptions;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import java.io.IOException;
import java.util.Map;
import java.util.NoSuchElementException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  public void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, message);
  }

  @ExceptionHandler({
    DuplicateItemException.class,
    ItemDeletionException.class,
    InsufficientStockException.class
  })
  public ResponseEntity<Map<String, String>> conflict(RuntimeException exception) {
    return error(HttpStatus.CONFLICT, exception.getMessage());
  }

  @ExceptionHandler({BookingException.class, IllegalArgumentException.class})
  public ResponseEntity<Map<String, String>> badRequest(RuntimeException exception) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<Map<String, String>> notFound(NoSuchElementException exception) {
    return error(HttpStatus.NOT_FOUND, exception.getMessage());
  }

  @ExceptionHandler(AuthenticationException.class)
  public ResponseEntity<Map<String, String>> unauthenticated(AuthenticationException exception) {
    return error(HttpStatus.UNAUTHORIZED, exception.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, String>> accessDenied(AccessDeniedException exception) {
    return error(HttpStatus.UNAUTHORIZED, "Access denied");
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, String>> responseStatus(ResponseStatusException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(Map.of("message", message(exception.getReason(), "Request failed")));
  }

  @ExceptionHandler({
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class,
    MissingServletRequestParameterException.class,
    ConstraintViolationException.class
  })
  public ResponseEntity<Map<String, String>> invalidInput(Exception exception) {
    return error(HttpStatus.BAD_REQUEST, "Invalid request data");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> validation(MethodArgumentNotValidException exception) {
    String message =
        exception.getBindingResult().getFieldErrors().stream()
            .findFirst()
            .map(
                field ->
                    field.getField() + ": " + message(field.getDefaultMessage(), "Invalid value"))
            .orElse("Invalid request data");
    return error(HttpStatus.BAD_REQUEST, message);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<Map<String, String>> dataConflict(
      DataIntegrityViolationException exception) {
    return error(HttpStatus.CONFLICT, "The request conflicts with existing data");
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Map<String, String>> methodNotAllowed(
      HttpRequestMethodNotSupportedException exception) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported");
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<Map<String, String>> unsupportedMediaType(
      HttpMediaTypeNotSupportedException exception) {
    return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported");
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, String>> unexpected(Exception exception) {
    log.error("Unhandled API error", exception);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
  }

  private ResponseEntity<Map<String, String>> error(HttpStatus status, String message) {
    return ResponseEntity.status(status)
        .body(Map.of("message", message(message, status.getReasonPhrase())));
  }

  private String message(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value;
  }
}
