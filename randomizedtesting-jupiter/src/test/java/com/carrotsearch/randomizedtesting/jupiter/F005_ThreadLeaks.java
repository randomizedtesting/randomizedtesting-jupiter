package com.carrotsearch.randomizedtesting.jupiter;

import static com.carrotsearch.randomizedtesting.jupiter.infra.TestInfra.*;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.*;

import com.carrotsearch.randomizedtesting.jupiter.infra.IgnoreInStandaloneRuns;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Verify that {@link DetectThreadLeaks} detects threads leaked from tests. */
public class F005_ThreadLeaks {
  @Nested
  class TestSuiteScope {
    @TestFactory
    Stream<DynamicTest> leakedThreadIsDetectedAtSuiteEnd() {
      return Stream.of(
              LeakInBeforeAllMethod.class, LeakInTestMethod.class, LeakInAfterAllMethod.class)
          .map(
              clazz ->
                  DynamicTest.dynamicTest(
                      clazz.getSimpleName(),
                      () -> {
                        collectExecutionResults(testKitBuilder(clazz))
                            .results()
                            .allEvents()
                            .finished()
                            .failed()
                            .assertEventsMatchExactly(
                                event(finishedWithFailure(instanceOf(AssertionError.class))));
                      }));
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class LeakInTestMethod extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread();
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class LeakInAfterAllMethod extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {}

      @AfterAll
      static void afterAll() {
        startSleepingThread();
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class LeakInBeforeAllMethod extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {}

      @BeforeAll
      static void beforeAll() {
        startSleepingThread();
      }
    }
  }

  @Nested
  class TestTestScope {
    @Test
    void leakedThreadIsDetectedAfterTest() {
      collectExecutionResults(testKitBuilder(TestScopeWithLeak.class))
          .results()
          .allEvents()
          .finished()
          .failed()
          .assertEventsMatchExactly(event(finishedWithFailure(instanceOf(AssertionError.class))));
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class TestScopeWithLeak extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread();
      }
    }
  }

  @Nested
  class TestConcurrentMode {
    @Test
    void leakedThreadDoesNotFailInConcurrentMode() {
      // In concurrent mode the extension is disabled: no AssertionErrors, even with a leak.
      collectExecutionResults(testKitBuilder(ConcurrentWithLeak.class))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    @Execution(ExecutionMode.CONCURRENT)
    static class ConcurrentWithLeak extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread();
      }
    }
  }

  /** Starts a daemon thread that sleeps long enough to be observable as a leak. */
  private static void startSleepingThread() {
    var t =
        new Thread(
            () -> {
              try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
              } catch (InterruptedException ignored) {
              }
            });
    t.setDaemon(true);
    t.start();
  }
}
