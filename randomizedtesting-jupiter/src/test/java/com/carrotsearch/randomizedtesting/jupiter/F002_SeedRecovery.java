package com.carrotsearch.randomizedtesting.jupiter;

import static com.carrotsearch.randomizedtesting.jupiter.infra.NestedTestResults.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.carrotsearch.randomizedtesting.jupiter.infra.NestedTest;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Verify that root seed (or the full seed chain) is recoverable in case of test errors. */
public class F002_SeedRecovery {
  @Test
  public void rootSeedIsAvailable() {
    assertThat(
            collectExecutionResults(
                    testKitBuilder(TestRootSeedIsAvailable.class)
                        .configurationParameter(
                            RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey,
                            "dead:beef:cafe"))
                .testResultsAsStatusStrings()
                .values())
        .containsOnly("testMethod: OK");
  }

  @Randomized
  static class TestRootSeedIsAvailable extends NestedTest {
    @Test
    void testMethod(RandomizedContext ctx) {
      Assertions.assertThat(ctx.getRootSeed().toString()).isEqualTo("DEAD");
    }
  }
}
