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

  @Nested
  class TestExcludeThreads {
    @Test
    void excludedThreadDoesNotFail() {
      collectExecutionResults(testKitBuilder(ExcludedByClassFilter.class))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @Test
    void nonExcludedThreadStillFails() {
      collectExecutionResults(testKitBuilder(NonExcludedStillFails.class))
          .results()
          .allEvents()
          .finished()
          .failed()
          .assertEventsMatchExactly(event(finishedWithFailure(instanceOf(AssertionError.class))));
    }

    @Test
    void methodAndClassFiltersStackHierarchically() {
      collectExecutionResults(testKitBuilder(HierarchicalFilters.class))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    // Class filter excludes "excluded-a-*"; the leaked thread matches → pass.
    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    @DetectThreadLeaks.ExcludeThreads(ExcludeNamedAFilter.class)
    static class ExcludedByClassFilter extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startNamedThread("excluded-a-1");
      }
    }

    // Class filter excludes "excluded-a-*"; leaked thread is unnamed → still detected → fail.
    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    @DetectThreadLeaks.ExcludeThreads(ExcludeNamedAFilter.class)
    static class NonExcludedStillFails extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread();
      }
    }

    // Class filter excludes "excluded-a-*", method filter excludes "excluded-b-*";
    // both threads are started → both excluded → pass.
    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    @DetectThreadLeaks.ExcludeThreads(ExcludeNamedAFilter.class)
    static class HierarchicalFilters extends IgnoreInStandaloneRuns {
      @Test
      @DetectThreadLeaks.ExcludeThreads(ExcludeNamedBFilter.class)
      void testMethod() {
        startNamedThread("excluded-a-1");
        startNamedThread("excluded-b-1");
      }
    }
  }

  /** Predicate that excludes threads whose names start with "excluded-a-". */
  public static class ExcludeNamedAFilter implements java.util.function.Predicate<Thread> {
    @Override
    public boolean test(Thread t) {
      return t.getName().startsWith("excluded-a-");
    }
  }

  /** Predicate that excludes threads whose names start with "excluded-b-". */
  public static class ExcludeNamedBFilter implements java.util.function.Predicate<Thread> {
    @Override
    public boolean test(Thread t) {
      return t.getName().startsWith("excluded-b-");
    }
  }

  private static void startNamedThread(String name) {
    var t =
        new Thread(
            () -> {
              try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
              } catch (InterruptedException ignored) {
              }
            },
            name);
    t.setDaemon(true);
    t.start();
  }

  @Nested
  class TestUncaughtExceptions {
    @Test
    void uncaughtExceptionFailsTheTest() {
      collectExecutionResults(testKitBuilder(UncaughtInTestMethod.class))
          .results()
          .allEvents()
          .finished()
          .failed()
          .assertEventsMatchExactly(
              event(
                  finishedWithFailure(
                      instanceOf(AssertionError.class),
                      new Condition<>(
                          t ->
                              t.getCause() instanceof RuntimeException rc
                                  && "uncaught-test-exception".equals(rc.getMessage()),
                          "cause is the original RuntimeException"))));
    }

    @Test
    void uncaughtExceptionsWithThreadLeaksAreNotReported() {
      collectExecutionResults(testKitBuilder(UncaughtWithLeak.class))
          .results()
          .allEvents()
          .finished()
          .failed()
          .assertEventsMatchExactly(
              event(
                  finishedWithFailure(
                      instanceOf(AssertionError.class),
                      new Condition<>(t -> t.getCause() == null, "cause is empty."))));
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class UncaughtInTestMethod extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() throws InterruptedException {
        var t =
            new Thread(
                () -> {
                  throw new RuntimeException("uncaught-test-exception");
                });
        t.start();
        t.join();
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class UncaughtWithLeak extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        var t1 =
            new Thread(
                () -> {
                  try {
                    Thread.sleep(TimeUnit.MINUTES.toMillis(1));
                  } catch (InterruptedException ignored) {
                    throw new RuntimeException("uncaught-test-exception");
                  }
                });
        t1.start();
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
