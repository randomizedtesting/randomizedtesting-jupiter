package com.carrotsearch.randomizedtesting.jupiter;

import static com.carrotsearch.randomizedtesting.jupiter.infra.NestedTestResults.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.carrotsearch.randomizedtesting.jupiter.infra.NestedTest;
import java.io.PrintWriter;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that {@link RandomizedContextSupplier} properly creates and injects a {@link
 * RandomizedContext} into test callbacks and methods.
 */
public class R001_RandomizedContextInjection {
  @Test
  public void fixedSeedAllHooks() {
    assertThat(
            collectExecutionResults(
                    testKitBuilder(FixedSeedAllHooks.class)
                        .configurationParameter(
                            RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey,
                            "dead:beef:cafe"))
                .testResultsAsStatusStrings()
                .values())
        .containsOnly("testMethod: OK");
  }

  @Randomized
  static class FixedSeedAllHooks extends NestedTest {
    public FixedSeedAllHooks(RandomizedContext ctx) {
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

  @Test
  public void randomSeedVariesInTestRepeats() {
    var executionResult = collectExecutionResults(testKitBuilder(RandomSeedInRepeatedTest.class));

    // root and class seeds should remain constant.
    Assertions.assertThat(
            executionResult.capturedOutput().values().stream()
                .map(s -> SeedChain.parse(s).seeds().subList(0, 2))
                .collect(Collectors.toSet()))
        .hasSize(1);

    // each test should have a unique last seed.
    Assertions.assertThat(
            executionResult.capturedOutput().values().stream()
                .map(s -> SeedChain.parse(s).seeds().get(2))
                .collect(Collectors.toSet()))
        .hasSizeBetween(8, 10); // allow some collisions. Not likely, but...
  }

  @Test
  public void classSeedChangesBetweenReruns() {
    var executionResult1 = collectExecutionResults(testKitBuilder(RandomSeedInRepeatedTest.class));
    var executionResult2 = collectExecutionResults(testKitBuilder(RandomSeedInRepeatedTest.class));

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
  static class RandomSeedInRepeatedTest extends NestedTest {
    @RepeatedTest(10)
    void testMethod(PrintWriter pw, RandomizedContext ctx) {
      pw.println(ctx.getSeedChain());
    }
  }

  @Test
  public void randomSeedVariesInParameterizedClassRepeats() {
    var executionResult =
        collectExecutionResults(testKitBuilder(RandomSeedInParameterizedClass.class));

    // the seed chain should be now composed of engine:class:class-invocation:* elements.
    // ensure they're unique for each invocation.
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
  static class RandomSeedInParameterizedClass extends NestedTest {
    @Test
    void testMethod(PrintWriter pw, RandomizedContext ctx) {
      pw.println(ctx.getSeedChain());
    }
  }

  // TODO: test filtering of a repeated test (from the same root seed) should rerun the same test.
  // TODO: stack trace augmentation in case of failed tests (seed).
}
