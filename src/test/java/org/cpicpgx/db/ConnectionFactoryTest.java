package org.cpicpgx.db;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * @author Ryan Whaley
 */
public class ConnectionFactoryTest {
  
  @Test
  public void testMakeJdbcUrl() {
    assertNotNull(ConnectionFactory.makeJdbcUrl());
  }
}
