package com.carrotsearch.randomizedtesting.jupiter;

import static com.carrotsearch.randomizedtesting.jupiter.infra.TestInfra.*;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.*;

import com.carrotsearch.randomizedtesting.jupiter.infra.IgnoreInStandaloneRuns;
import java.util.stream.Stream;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Verify that root seed (or the full seed chain) is recoverable in case of test errors. */
public class F002_SeedRecovery {
  @Nested
  class TestRootSeedIsAvailable {
    @Test
    public void checkAtMethodLevel() {
      collectExecutionResults(
              testKitBuilder(AtTestLevel.class)
                  .configurationParameter(
                      RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey, "dead"))
          .results()
          .testEvents()
          .finished()
          .assertEventsMatchExactly(event(test(), finishedSuccessfully()));
    }

    @Randomized
    static class AtTestLevel extends IgnoreInStandaloneRuns {
      @Test
      void testMethod(RandomizedContext ctx) {
        Assertions.assertThat(ctx.getRootSeed().toString()).isEqualTo("DEAD");
      }
    }
  }

  @Nested
  class TestFailuresWithStackTraceContainSeed {
    record Check(String name, Class<?> clazz, String expectedSeed) {}

    @TestFactory
    Stream<DynamicTest> dynamicTestsFromIterator() {
      return Stream.of(
              new Check("@BeforeAll", AtBeforeAll.class, "[DEAD:BEEF]"),
              new Check("@BeforeEach", AtBeforeEachLevel.class, "[DEAD:BEEF:CAFE]"),
              new Check("@Test", AtTestLevel.class, "[DEAD:BEEF:CAFE]"),
              new Check("@AfterEach", AtAfterEachLevel.class, "[DEAD:BEEF:CAFE]"),
              new Check("@AfterAll", AtAfterAll.class, "[DEAD:BEEF]"))
          .map(e -> DynamicTest.dynamicTest(e.name(), () -> runCheck(e)));
    }

    private void runCheck(Check e) {
      collectExecutionResults(
              testKitBuilder(e.clazz())
                  .configurationParameter(
                      RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey, "dead:beef:cafe"))
          .results()
          .allEvents()
          .finished()
          .failed()
          .assertEventsMatchExactly(
              event(
                  finishedWithFailure(
                      instanceOf(AssertionError.class),
                      message("Failure."),
                      new Condition<>(
                          t -> {
                            Assertions.assertThat(t.getStackTrace()[0].toString())
                                .contains(
                                    RandomizedContextSupplier.AUGMENTED_SEED_CLASS
                                        + ".seed("
                                        + e.expectedSeed
                                        + ")");
                            return true;
                          },
                          "first stack frame contains seed entry"))));
    }

    @Randomized
    static class AtBeforeAll extends IgnoreInStandaloneRuns {
      @BeforeAll
      static void beforeAll() {
        throw new AssertionError("Failure.");
      }

      @Test
      void testMethod() {}
    }

    @Randomized
    static class AtBeforeEachLevel extends IgnoreInStandaloneRuns {
      @BeforeEach
      void beforeEach() {
        throw new AssertionError("Failure.");
      }

      @Test
      void testMethod() {}
    }

    @Randomized
    static class AtTestLevel extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        throw new AssertionError("Failure.");
      }
    }

    @Randomized
    static class AtAfterEachLevel extends IgnoreInStandaloneRuns {
      @AfterEach
      void afterEach() {
        throw new AssertionError("Failure.");
      }

      @Test
      void testMethod() {}
    }

    @Randomized
    static class AtAfterAll extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {}

      @BeforeAll
      static void afterAll() {
        throw new AssertionError("Failure.");
      }
    }
  }
}
