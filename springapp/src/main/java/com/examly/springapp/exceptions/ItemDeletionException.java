package com.examly.springapp.exceptions;

public class ItemDeletionException extends RuntimeException {
  public ItemDeletionException(String message) {
    super(message);
  }
}
