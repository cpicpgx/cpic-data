package org.cpicpgx.exception;

/**
 * Exception that is thrown when some expected thing is not in state 
 *
 * @author Ryan Whaley
 */
public class NotFoundException extends Throwable {
  
  public NotFoundException(String message) {
    super(message);
  }
}
