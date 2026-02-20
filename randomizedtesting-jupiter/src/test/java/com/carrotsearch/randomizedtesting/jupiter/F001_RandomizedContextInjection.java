package com.carrotsearch.randomizedtesting.jupiter;

import static com.carrotsearch.randomizedtesting.jupiter.infra.TestInfra.*;
import static org.junit.platform.testkit.engine.EventConditions.*;

import com.carrotsearch.randomizedtesting.jupiter.infra.IgnoreInStandaloneRuns;
import java.io.PrintWriter;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.platform.engine.discovery.DiscoverySelectors;

/**
 * Verifies that {@link RandomizedContextSupplier} properly creates and injects a {@link
 * RandomizedContext} into test callbacks and methods.
 */
public class F001_RandomizedContextInjection {
  @Nested
  class TestFixedSeedInjection {
    @Test
    public void testAllHooks() {
      collectExecutionResults(
              testKitBuilder(T1.class)
                  .configurationParameter(
                      RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey, "dead:beef:cafe"))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @Randomized
    static class T1 extends IgnoreInStandaloneRuns {
      public T1(RandomizedContext ctx) {
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

  @Nested
  class SeedIsRandomized {
    @Test
    public void randomSeedVariesInRepeatedTests() {
      var executionResult = collectExecutionResults(testKitBuilder(RepeatedTests.class));

      // root and class seeds should remain constant.
      Assertions.assertThat(
              executionResult.capturedOutput().values().stream()
                  .map(s -> SeedChain.parse(s).seeds().subList(0, 2))
                  .collect(Collectors.toSet()))
          .hasSize(1);

      // each test should have a unique last seed.
      Assertions.assertThat(
              executionResult.capturedOutput().values().stream()
                  .map(
                      s -> {
                        var seeds = SeedChain.parse(s).seeds();
                        // seed chain: [engine:class:test-template:method-invocation]
                        Assertions.assertThat(seeds).hasSize(4);
                        return seeds.getLast();
                      })
                  .collect(Collectors.toSet()))
          .hasSizeBetween(8, 10); // allow some collisions. Not likely, but...
    }

    @Test
    public void classSeedChangesBetweenReruns() {
      var executionResult1 = collectExecutionResults(testKitBuilder(RepeatedTests.class));
      var executionResult2 = collectExecutionResults(testKitBuilder(RepeatedTests.class));

      Assertions.assertThat(
              Stream.of(executionResult1, executionResult2)
                  .map(
                      r ->
                          r.capturedOutput().values().stream()
                              .map(s -> SeedChain.parse(s).seeds().subList(0, 2))
                              .collect(Collectors.toSet())))
          .hasSize(2);
    }

    @Randomized
    static class RepeatedTests extends IgnoreInStandaloneRuns {
      @RepeatedTest(10)
      void testMethod(PrintWriter pw, RandomizedContext ctx) {
        pw.println(ctx.getSeedChain());
      }
    }

    @Test
    public void randomSeedInParameterizedClassRepeats() {
      var executionResult = collectExecutionResults(testKitBuilder(ParameterizedClassTests.class));

      // seed chain: [engine:class:class-template:method]
      // ensure different seeds for each test invocation.
      Assertions.assertThat(
              executionResult.capturedOutput().values().stream()
                  .map(
                      s -> {
                        var seeds = SeedChain.parse(s).seeds();
                        Assertions.assertThat(seeds).hasSize(4);
                        return SeedChain.parse(s).seeds().subList(0, 3);
                      })
                  .collect(Collectors.toSet()))
          .hasSizeBetween(8, 10); // allow for collisions. Not likely, but...
    }

    @Randomized
    @ParameterizedClass
    @ValueSource(strings = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"})
    static class ParameterizedClassTests extends IgnoreInStandaloneRuns {
      @Test
      void testMethod(PrintWriter pw, RandomizedContext ctx) {
        pw.println(ctx.getSeedChain());
      }
    }
  }

  @Nested
  class TestFilteringDoesNotAffectSeed {
    @Test
    public void identicalDerivedSeedWithTestFiltering() {
      var executionResult1 =
          collectExecutionResults(
              testKitBuilder(TestIdenticalRandomWithTestFiltering.class)
                  .configurationParameter(
                      RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey, "deadbeed"));

      // select just one of the tests and pick it by its unique id. then re-run it.
      // the seed/ random should be identical because the re-run starts from the same root
      // seed.
      var pickedTest = executionResult1.capturedOutput().keySet().toArray(String[]::new)[2];

      var executionResult2 =
          collectExecutionResults(
              testKitBuilder()
                  .configurationParameter(
                      RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey, "deadbeed")
                  .selectors(DiscoverySelectors.selectUniqueId(pickedTest)));

      String o1 = executionResult1.capturedOutput().get(pickedTest);
      String o2 = executionResult2.capturedOutput().get(pickedTest);
      Assertions.assertThat(o1).isEqualTo(o2);
    }

    @Randomized
    static class TestIdenticalRandomWithTestFiltering extends IgnoreInStandaloneRuns {
      @RepeatedTest(10)
      void testMethod(PrintWriter pw, RandomizedContext ctx) {
        pw.println(ctx.getSeedChain());
        pw.println(
            IntStream.range(0, 10)
                .mapToObj(i -> Long.toHexString(ctx.getRandom().nextLong()))
                .collect(Collectors.joining(":")));
      }
    }
  }
}
