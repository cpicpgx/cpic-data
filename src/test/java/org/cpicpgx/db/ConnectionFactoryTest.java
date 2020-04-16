package org.cpicpgx.db;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Quick check to make sure environment variables are being translated to DB connection string
 *
 * @author Ryan Whaley
 */
public class ConnectionFactoryTest {
  
  @Test
  public void testMakeJdbcUrl() {
    assertNotNull(ConnectionFactory.makeJdbcUrl());
    assertNotNull(ConnectionFactory.getUser());
  }
}
