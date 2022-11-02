package com.db.awmd.challenge.exception;

import javafx.beans.binding.StringExpression;

public class AccountNotFoundException extends RuntimeException {

  public AccountNotFoundException(StringExpression message) {
    super(String.valueOf(message));
  }
}
