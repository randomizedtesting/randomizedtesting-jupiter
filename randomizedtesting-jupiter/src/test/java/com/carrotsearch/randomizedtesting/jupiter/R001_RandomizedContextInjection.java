package com.carrotsearch.randomizedtesting.jupiter;

import static com.carrotsearch.randomizedtesting.jupiter.infra.NestedTestResults.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.carrotsearch.randomizedtesting.jupiter.infra.NestedTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link RandomizedContextSupplier} properly creates and injects a {@link
 * RandomizedContext} into test callbacks and methods.
 */
public class R001_RandomizedContextInjection {
  @Test
  public void contextParameterInjection() {
    assertThat(
            collectExecutionResults(
                    testKitBuilder(RandomizedContextInjection.class)
                        .configurationParameter(
                            RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey,
                            "dead:beef:cafe"))
                .testResultsAsStatusStrings()
                .values())
        .containsOnly("testMethod: OK");
  }

  @Randomized
  static class RandomizedContextInjection extends NestedTest {
    public RandomizedContextInjection(RandomizedContext ctx) {
      Assertions.assertThat(ctx.getSeedChain().toString()).isEqualTo("[DEAD:BEEF]");
    }

    @BeforeAll
    static void beforeAll(RandomizedContext ctx) {
      Assertions.assertThat(ctx.getSeedChain().toString()).isEqualTo("[DEAD:BEEF]");
    }

    @BeforeEach
    void beforeEach(RandomizedContext ctx) {
      Assertions.assertThat(ctx.getSeedChain().toString()).isEqualTo("[DEAD:BEEF:CAFE]");
    }

    @Test
    void testMethod(RandomizedContext ctx) {
      Assertions.assertThat(ctx.getSeedChain().toString()).isEqualTo("[DEAD:BEEF:CAFE]");
    }

    @AfterEach
    void afterEach(RandomizedContext ctx) {
      Assertions.assertThat(ctx.getSeedChain().toString()).isEqualTo("[DEAD:BEEF:CAFE]");
    }

    @AfterAll
    static void afterAll(RandomizedContext ctx) {
      Assertions.assertThat(ctx.getSeedChain().toString()).isEqualTo("[DEAD:BEEF]");
    }
  }
}
