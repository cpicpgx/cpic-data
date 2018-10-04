package org.cpicpgx.exception;

/**
 * Exception that is thrown when some expected thing is not in state 
 *
 * @author Ryan Whaley
 */
public class NotFoundException extends Exception {
  
  public NotFoundException(String message) {
    super(message);
  }
}
