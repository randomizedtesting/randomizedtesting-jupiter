package com.carrotsearch.randomizedtesting.tests;

import static com.carrotsearch.randomizedtesting.tests.infra.TestInfra.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedSuccessfully;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;

import com.carrotsearch.randomizedtesting.jupiter.Randomized;
import com.carrotsearch.randomizedtesting.jupiter.RandomizedContext;
import com.carrotsearch.randomizedtesting.jupiter.RandomizedTestEngine;
import com.carrotsearch.randomizedtesting.jupiter.SysProps;
import com.carrotsearch.randomizedtesting.tests.infra.IgnoreInStandaloneRuns;
import java.io.PrintWriter;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Verifies that {@link com.carrotsearch.randomizedtesting.jupiter.RandomizedTestEngine} multiplies
 * test execution by iteration count.
 */
public class F004_TestEngineIterations {
  @Nested
  class TestIterationMultiplier {
    @Test
    void testsRunOnceByDefault() {
      var result =
          collectExecutionResults(
              EngineTestKit.engine(new RandomizedTestEngine())
                  .configurationParameter("nested-integration-test", "true")
                  .selectors(selectClass(SimpleTest.class)));

      result
          .results()
          .testEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()))
          .haveExactly(1, event(test(), finishedSuccessfully()));
    }

    @Test
    void testsAreMultipliedByIterationCount() {
      var result =
          collectExecutionResults(
              EngineTestKit.engine(new RandomizedTestEngine())
                  .configurationParameter(RandomizedTestEngine.ITERATIONS_PROPERTY, "3")
                  .configurationParameter("nested-integration-test", "true")
                  .selectors(selectClass(SimpleTest.class)));

      result.results().testEvents().assertStatistics(s -> s.finished(3).succeeded(3));
    }

    @Test
    void seedsDifferAcrossIterationsWithFixedRootSeed() {
      var result =
          collectExecutionResults(
              EngineTestKit.engine(new RandomizedTestEngine())
                  .configurationParameter(RandomizedTestEngine.ITERATIONS_PROPERTY, "3")
                  .configurationParameter("nested-integration-test", "true")
                  .configurationParameter(SysProps.TESTS_SEED.propertyKey, "DEAD")
                  .selectors(selectClass(SimpleTest.class)));

      result.results().testEvents().assertStatistics(s -> s.finished(3).succeeded(3));

      // Each iteration derives a different seed because its unique ID contains the iteration index.
      Assertions.assertThat(result.capturedOutput().values().stream().distinct())
          .as("seeds should differ across iterations even with a fixed root seed")
          .hasSize(3);
    }

    @Randomized
    static class SimpleTest extends IgnoreInStandaloneRuns {
      @Test
      void test(PrintWriter pw, RandomizedContext ctx) {
        pw.println(ctx.getSeedChain());
      }
    }
  }
}
