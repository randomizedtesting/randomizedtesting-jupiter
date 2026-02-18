package com.carrotsearch.randomizedtesting.jupiter.experiments;

import com.carrotsearch.randomizedtesting.jupiter.Randomized;
import com.carrotsearch.randomizedtesting.jupiter.RandomizedContext;
import com.carrotsearch.randomizedtesting.jupiter.RandomizedContextSupplier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that {@link RandomizedContextSupplier} properly creates and injects a {@link
 * RandomizedContext} into test methods.
 */
@Disabled
@ParameterizedClass
@ValueSource(strings = {"racecar", "radar", "able was I ere I saw elba"})
@Randomized
public class TestExperiments {
  @BeforeAll
  public static void beforeAll(RandomizedContext context) {
    System.out.println("BeforeAll: " + context);
  }

  @Test
  public void b(RandomizedContext context) {
    System.out.println("Test b: " + context);
  }

  @RepeatedTest(5)
  public void a(RandomizedContext context) {
    System.out.println("Test a: " + context);
  }
}
