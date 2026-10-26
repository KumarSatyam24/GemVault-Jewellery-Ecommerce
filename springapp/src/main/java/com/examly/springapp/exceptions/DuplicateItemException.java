package com.examly.springapp.exceptions;

public class DuplicateItemException extends RuntimeException {
  public DuplicateItemException(String message) {
    super(message);
  }
}
