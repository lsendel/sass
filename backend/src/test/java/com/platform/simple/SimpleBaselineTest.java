package com.platform.simple;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Simple baseline test to establish JaCoCo coverage reporting */
class SimpleBaselineTest {

  private static final int HELLO_WORLD_LENGTH = 11;

  @Test
  @DisplayName("Simple test to establish coverage baseline")
  void testBaseline() {
    // Simple test that always passes to establish baseline
    assertTrue(true);
    assertEquals(1, 1);
    assertNotNull("test");
  }

  @Test
  @DisplayName("Test basic string operations")
  void testStringOperations() {
    String test = "Hello World";
    assertEquals(HELLO_WORLD_LENGTH, test.length());
    assertTrue(test.contains("World"));
    assertFalse(test.isEmpty());
  }
}
