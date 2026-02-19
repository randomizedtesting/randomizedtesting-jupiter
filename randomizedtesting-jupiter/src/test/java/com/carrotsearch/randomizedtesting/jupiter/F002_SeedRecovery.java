package com.carrotsearch.randomizedtesting.jupiter;

import static com.carrotsearch.randomizedtesting.jupiter.infra.NestedTestResults.*;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.*;

import com.carrotsearch.randomizedtesting.jupiter.infra.NestedTest;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

/** Verify that root seed (or the full seed chain) is recoverable in case of test errors. */
public class F002_SeedRecovery {
  @Test
  public void rootSeedIsAvailable() {
    collectExecutionResults(
            testKitBuilder(TestRootSeedIsAvailable.class)
                .configurationParameter(
                    RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey, "dead"))
        .results()
        .testEvents()
        .finished()
        .assertEventsMatchExactly(event(test(), finishedSuccessfully()));
  }

  @Randomized
  static class TestRootSeedIsAvailable extends NestedTest {
    @Test
    void testMethod(RandomizedContext ctx) {
      Assertions.assertThat(ctx.getRootSeed().toString()).isEqualTo("DEAD");
    }
  }

  @Test
  public void stackEntryAddedForFailedTests() {
    collectExecutionResults(
            testKitBuilder(TestStackEntryAddedForFailedTests.class)
                .configurationParameter(
                    RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey, "dead:beef:cafe"))
        .results()
        .testEvents()
        .finished()
        .assertEventsMatchExactly(
            event(
                test(),
                finishedWithFailure(
                    instanceOf(AssertionError.class),
                    message("Failure."),
                    new Condition<>(
                        t ->
                            t.getStackTrace()[0]
                                .toString()
                                .contains(
                                    RandomizedContextSupplier.AUGMENTED_SEED_CLASS
                                        + ".seed([DEAD:BEEF:CAFE])"),
                        "first stack frame matches seed entry"))));
  }

  @Randomized
  static class TestStackEntryAddedForFailedTests extends NestedTest {
    @Test
    void testMethod() {
      throw new AssertionError("Failure.");
    }
  }
}
