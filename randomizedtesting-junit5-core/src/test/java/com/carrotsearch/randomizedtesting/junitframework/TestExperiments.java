package com.carrotsearch.randomizedtesting.junitframework;


import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies that {@link RandomizedContextSupplier} properly creates and injects a {@link
 * RandomizedContext} into test methods.
 */
@Randomized
public class TestExperiments {
  @RepeatedTest(10)
  //@Test
  public void contextParameterInjection() {
    System.out.println("Test.");
  }
}
