package org.cpicpgx.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Ryan Whaley
 */
public class ConnectionFactoryTest {
  
  @Test
  public void testMakeJdbcUrl() {
    assertNotNull(ConnectionFactory.makeJdbcUrl());
  }
}
