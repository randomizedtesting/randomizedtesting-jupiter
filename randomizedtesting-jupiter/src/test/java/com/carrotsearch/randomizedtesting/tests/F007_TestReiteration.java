package com.carrotsearch.randomizedtesting.tests;

import static com.carrotsearch.randomizedtesting.tests.infra.TestInfra.*;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;

import com.carrotsearch.randomizedtesting.jupiter.Randomized;
import com.carrotsearch.randomizedtesting.jupiter.RandomizedContext;
import com.carrotsearch.randomizedtesting.jupiter.RepeatExecutionTestEngine;
import com.carrotsearch.randomizedtesting.jupiter.SeedChain;
import com.carrotsearch.randomizedtesting.jupiter.SysProps;
import com.carrotsearch.randomizedtesting.tests.infra.IgnoreInStandaloneRuns;
import com.carrotsearch.randomizedtesting.tests.infra.TestInfra;
import java.io.PrintWriter;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

/** Verifies that {@link RepeatExecutionTestEngine} correctly multiplies test execution. */
public class F007_TestReiteration {
  @Test
  void noReiterationsByDefault() {
    var result =
        collectExecutionResults(
            TestInfra.testKitBuilder(RepeatExecutionTestEngine.ENGINE_ID)
                .selectors(selectClass(SimpleTest.class)));

    result
        .results()
        .testEvents()
        .assertThatEvents()
        .doNotHave(event(finishedWithFailure()))
        .haveExactly(0, event(test()));
  }

  @Test
  void testsAreMultipliedByIterationCount() {
    var result =
        collectExecutionResults(
            TestInfra.testKitBuilder(RepeatExecutionTestEngine.ENGINE_ID)
                .configurationParameter(SysProps.TESTS_ITERS.propertyKey, "3")
                .selectors(selectClass(SimpleTest.class)));

    result.results().testEvents().assertStatistics(s -> s.finished(3).succeeded(3));
  }

  @Test
  void seedsAreIdenticalAcrossIterationsWithFixedRootSeed() {
    var iterations = 5;
    var result =
        collectExecutionResults(
            TestInfra.testKitBuilder(RepeatExecutionTestEngine.ENGINE_ID)
                .configurationParameter(SysProps.TESTS_ITERS.propertyKey, "" + iterations)
                .configurationParameter(SysProps.TESTS_SEED.propertyKey, "DEADBEEF")
                .selectors(selectClass(SimpleTest.class)));

    result
        .results()
        .testEvents()
        .assertStatistics(s -> s.finished(iterations).succeeded(iterations));

    // Each iteration should have the same seed chain because we strip the top-level reiteration
    // segments.
    Assertions.assertThat(result.capturedOutput().values().stream().distinct())
        .as("seed chains should be the same across iterations")
        .hasSize(1);

    Assertions.assertThat(
            result.capturedOutput().values().stream()
                .map(value -> value.split("\\s")[0])
                .map(v -> SeedChain.parse(v))
                .map(chain -> chain.seeds().getFirst().toString()))
        .allMatch(v -> v.equals("DEADBEEF"));
  }

  @Test
  void seedsAreDifferentAcrossIterationsWithNoRootSeed() {
    var iterations = 5;
    var result =
        collectExecutionResults(
            TestInfra.testKitBuilder(RepeatExecutionTestEngine.ENGINE_ID)
                .configurationParameter(SysProps.TESTS_ITERS.propertyKey, "" + iterations)
                .selectors(selectClass(SimpleTest.class)));

    result
        .results()
        .testEvents()
        .assertStatistics(s -> s.finished(iterations).succeeded(iterations));

    // Each iteration should have a random root seed if there is no top-level fixed seed.
    Assertions.assertThat(result.capturedOutput().values().stream().distinct())
        .as("seed chains should be different across iterations")
        .hasSizeBetween(iterations - 2, iterations);
  }

  @Test
  void randomnessIsIdenticalForJupiterAndReiteratedTests() {
    List<String> jupiterResults;
    List<String> repeatedExecutionResults;

    {
      var result =
          collectExecutionResults(
              TestInfra.testKitBuilder()
                  .configurationParameter(SysProps.TESTS_SEED.propertyKey, "DEADBEEF")
                  .selectors(selectClass(SimpleTest.class)));

      result.results().testEvents().assertStatistics(s -> s.finished(1).succeeded(1));

      jupiterResults = result.capturedOutput().values().stream().toList();
    }

    {
      var result =
          collectExecutionResults(
              TestInfra.testKitBuilder(RepeatExecutionTestEngine.ENGINE_ID)
                  .configurationParameter(SysProps.TESTS_ITERS.propertyKey, "1")
                  .configurationParameter(SysProps.TESTS_SEED.propertyKey, "DEADBEEF")
                  .selectors(selectClass(SimpleTest.class)));

      result.results().testEvents().assertStatistics(s -> s.finished(1).succeeded(1));

      repeatedExecutionResults = result.capturedOutput().values().stream().toList();
    }

    Assertions.assertThat(jupiterResults).containsExactlyElementsOf(repeatedExecutionResults);
  }

  @Randomized
  static class SimpleTest extends IgnoreInStandaloneRuns {
    @Test
    void test(PrintWriter pw, RandomizedContext ctx) {
      pw.println(ctx.getSeedChain() + " " + ctx.getRandom().nextLong());
    }
  }
}
