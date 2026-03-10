package com.carrotsearch.randomizedtesting.jupiter;

import static com.carrotsearch.randomizedtesting.jupiter.infra.TestInfra.*;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.*;

import com.carrotsearch.randomizedtesting.jupiter.infra.IgnoreInStandaloneRuns;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
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
  class TestStackTracesInMessage {
    @Test
    void leakErrorMessageContainsStackTrace() {
      collectExecutionResults(testKitBuilder(SuiteScopeWithLeak.class))
          .results()
          .allEvents()
          .finished()
          .failed()
          .assertEventsMatchExactly(
              event(
                  finishedWithFailure(
                      instanceOf(AssertionError.class),
                      new Condition<>(
                          t -> t.getMessage().contains(getClass().getPackageName()),
                          "error message contains stack frames"))));
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class SuiteScopeWithLeak extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
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
  class TestLinger {
    @Test
    void threadTerminatingWithinLingerWindowPasses() {
      collectExecutionResults(testKitBuilder(ShortLivedLeak.class))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @Test
    void methodLingerTakesPrecedenceOverAbsentClassLinger() {
      collectExecutionResults(testKitBuilder(MethodLingerOverridesAbsentClassLinger.class))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @Test
    void methodLingerTakesPrecedenceOverClassLinger() {
      collectExecutionResults(testKitBuilder(MethodLingerOverridesClassLinger.class))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @Test
    void threadOutlastingLingerWindowFails() {
      collectExecutionResults(testKitBuilder(LongLivedLeak.class))
          .results()
          .allEvents()
          .finished()
          .failed()
          .assertEventsMatchExactly(event(finishedWithFailure(instanceOf(AssertionError.class))));
    }

    // Class linger 10s; thread sleeps 100ms → terminates before linger expires → pass.
    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    @DetectThreadLeaks.LingerTime(millis = 10_000)
    static class ShortLivedLeak extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        var t =
            new Thread(
                () -> {
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException ignored) {
                  }
                });
        t.setDaemon(true);
        t.start();
      }
    }

    // Class linger 50ms; thread sleeps 1 min → outlasts linger → fail.
    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    @DetectThreadLeaks.LingerTime(millis = 50)
    static class LongLivedLeak extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread();
      }
    }

    // Method linger 10s overrides absent class linger; thread sleeps 100ms → pass.
    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class MethodLingerOverridesAbsentClassLinger extends IgnoreInStandaloneRuns {
      @Test
      @DetectThreadLeaks.LingerTime(millis = 10_000)
      void testMethod() {
        var t =
            new Thread(
                () -> {
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException ignored) {
                  }
                });
        t.setDaemon(true);
        t.start();
      }
    }

    // Method linger 10s overrides class linger 50ms; thread sleeps 100ms → pass.
    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    @DetectThreadLeaks.LingerTime(millis = 50)
    static class MethodLingerOverridesClassLinger extends IgnoreInStandaloneRuns {
      @Test
      @DetectThreadLeaks.LingerTime(millis = 10_000)
      void testMethod() {
        var t =
            new Thread(
                () -> {
                  try {
                    Thread.sleep(100);
                  } catch (InterruptedException ignored) {
                  }
                });
        t.setDaemon(true);
        t.start();
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
