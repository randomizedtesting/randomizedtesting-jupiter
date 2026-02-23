package com.carrotsearch.randomizedtesting.jupiter;

import static com.carrotsearch.randomizedtesting.jupiter.infra.TestInfra.*;
import static org.junit.platform.testkit.engine.EventConditions.*;

import com.carrotsearch.randomizedtesting.jupiter.infra.IgnoreInStandaloneRuns;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Verifies that {@link java.util.Random} instances are properly injected as parameters. */
public class F003_RandomInjection {
  @Nested
  class TestRandomInjection {
    @Test
    public void testAllHooks() {
      collectExecutionResults(testKitBuilder(T1.class))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @Randomized
    static class T1 extends IgnoreInStandaloneRuns {
      public T1(Random random) {
        Assertions.assertThat(random).isNotNull();
      }

      @BeforeAll
      static void beforeAll(Random random) {
        Assertions.assertThat(random).isNotNull();
      }

      @BeforeEach
      void beforeEach(Random random) {
        Assertions.assertThat(random).isNotNull();
      }

      @Test
      void testMethod(Random random) {
        Assertions.assertThat(random).isNotNull();
      }

      @AfterEach
      void afterEach(Random random) {
        Assertions.assertThat(random).isNotNull();
      }

      @AfterAll
      static void afterAll(Random random) {
        Assertions.assertThat(random).isNotNull();
      }
    }
  }

  @Nested
  class TestRandomFactoryAndState {
    @Test
    public void randomInitializedWithContextSeed() {
      var executionResults =
          IntStream.range(0, 5)
              .mapToObj(
                  unused ->
                      collectExecutionResults(
                          testKitBuilder(T1.class)
                              .configurationParameter(
                                  RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey,
                                  "deadbeef")))
              .toList();

      Assertions.assertThat(
              executionResults.stream()
                  .flatMap(r -> r.capturedOutput().values().stream())
                  .collect(Collectors.toSet()))
          .hasSize(1);
    }

    @TestFactory
    public Stream<DynamicTest> checkAllRandomFactories() {
      return Stream.of(RandomizedContextSupplier.RandomFactoryType.values())
          .map(
              t -> {
                return DynamicTest.dynamicTest(
                    t.name(),
                    () -> {
                      var expectedClass = t.get().apply(0).getClass().getName();

                      var executionResult =
                          collectExecutionResults(
                              testKitBuilder(T1.class)
                                  .configurationParameter(
                                      RandomizedContextSupplier.SysProps.TESTS_RANDOM_FACTORY
                                          .propertyKey,
                                      t.name().toLowerCase(Locale.ROOT))
                                  .configurationParameter(
                                      RandomizedContextSupplier.SysProps.TESTS_RANDOM_ASSERTING
                                          .propertyKey,
                                      "false"));
                      executionResult
                          .results()
                          .allEvents()
                          .assertThatEvents()
                          .doNotHave(event(finishedWithFailure()));

                      Assertions.assertThat(executionResult.capturedOutput().values())
                          .containsOnly(expectedClass);
                    });
              });
    }

    @Randomized
    static class T1 extends IgnoreInStandaloneRuns {
      @Test
      void testMethod(PrintWriter pw, Random rnd) {
        pw.print(rnd.getClass().getName());
      }
    }
  }

  @Nested
  class TestRandomAssertions {
    @Test
    public void testAllHooks() {
      collectExecutionResults(testKitBuilder(T1.class))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @Randomized
    static class T1 extends IgnoreInStandaloneRuns {
      @Test
      void testMethod(Random random) throws Exception {
        var ex = new AtomicReference<Exception>();
        var thread =
            new Thread(
                () -> {
                  try {
                    random.nextLong();
                  } catch (Exception e) {
                    ex.set(e);
                  }
                });
        thread.start();
        thread.join();

        Assertions.assertThat(ex.get())
            .isNotNull()
            .isExactlyInstanceOf(RuntimeException.class)
            .hasMessageContaining("This Random instance is tied to thread");
      }
    }
  }
}
