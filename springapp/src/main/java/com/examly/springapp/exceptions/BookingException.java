package com.examly.springapp.exceptions;

public class BookingException extends RuntimeException {
  public BookingException(String message) {
    super(message);
  }
}
