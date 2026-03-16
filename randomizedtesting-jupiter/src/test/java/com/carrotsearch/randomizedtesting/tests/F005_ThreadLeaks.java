package com.carrotsearch.randomizedtesting.tests;

import static com.carrotsearch.randomizedtesting.tests.infra.TestInfra.*;
import static org.junit.platform.testkit.engine.EventConditions.*;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.*;

import com.carrotsearch.randomizedtesting.jupiter.DetectThreadLeaks;
import com.carrotsearch.randomizedtesting.tests.infra.IgnoreInStandaloneRuns;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** Verify that {@link DetectThreadLeaks} detects threads leaked from tests. */
@Execution(ExecutionMode.SAME_THREAD)
public class F005_ThreadLeaks {
  private static final List<Thread> forkedThreads = new ArrayList<>();

  @AfterEach
  void interruptAndJoinForkedThreads() throws InterruptedException {
    for (var t : forkedThreads) t.interrupt();
    for (var t : forkedThreads) t.join();
    forkedThreads.clear();
  }

  @Nested
  class TestSuiteScope {
    @TestFactory
    Stream<DynamicTest> leakedThreadIsDetected() {
      return Stream.of(
              LeakInBeforeAll.class,
              LeakInBeforeEach.class,
              LeakInConstructor.class,
              LeakInTestMethod.class,
              LeakInAfterEach.class,
              LeakInAfterAll.class)
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
    static class LeakInBeforeAll extends IgnoreInStandaloneRuns {
      @BeforeAll
      static void beforeAll() {
        startSleepingThread();
      }

      @Test
      void testMethod() {}
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class LeakInBeforeEach extends IgnoreInStandaloneRuns {
      @BeforeEach
      void beforeEach() {
        startSleepingThread();
      }

      @Test
      void testMethod() {}
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class LeakInConstructor extends IgnoreInStandaloneRuns {
      LeakInConstructor() {
        startSleepingThread();
      }

      @Test
      void testMethod() {}
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class LeakInTestMethod extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread();
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class LeakInAfterEach extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {}

      @AfterEach
      void afterEach() {
        startSleepingThread();
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class LeakInAfterAll extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {}

      @AfterAll
      static void afterAll() {
        startSleepingThread();
      }
    }
  }

  @Nested
  class TestTestScope {
    @TestFactory
    Stream<DynamicTest> leakedThreadIsDetected() {
      return Stream.of(
              LeakInBeforeAll.class,
              LeakInBeforeEach.class,
              LeakInConstructor.class,
              LeakInTestMethod.class,
              LeakInAfterEach.class,
              LeakInAfterAll.class)
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

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class LeakInBeforeAll extends IgnoreInStandaloneRuns {
      @BeforeAll
      static void beforeAll() {
        startSleepingThread();
      }

      @Test
      void testMethod() {}
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class LeakInBeforeEach extends IgnoreInStandaloneRuns {
      @BeforeEach
      void beforeEach() {
        startSleepingThread();
      }

      @Test
      void testMethod() {}
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class LeakInConstructor extends IgnoreInStandaloneRuns {
      LeakInConstructor() {
        startSleepingThread();
      }

      @Test
      void testMethod() {}
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class LeakInTestMethod extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread();
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class LeakInAfterEach extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {}

      @AfterEach
      void afterEach() {
        startSleepingThread();
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class LeakInAfterAll extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {}

      @AfterAll
      static void afterAll() {
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
                          t -> t.getMessage().contains(getClass().getSimpleName()),
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

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    @DetectThreadLeaks.LingerTime(millis = 10_000)
    static class ShortLivedLeak extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread(Duration.ofMillis(100));
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    @DetectThreadLeaks.LingerTime(millis = 50)
    static class LongLivedLeak extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread();
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class MethodLingerOverridesAbsentClassLinger extends IgnoreInStandaloneRuns {
      @Test
      @DetectThreadLeaks.LingerTime(millis = 10_000)
      void testMethod() {
        startSleepingThread(Duration.ofMillis(100));
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    @DetectThreadLeaks.LingerTime(millis = 50)
    static class MethodLingerOverridesClassLinger extends IgnoreInStandaloneRuns {
      @Test
      @DetectThreadLeaks.LingerTime(millis = 10_000)
      void testMethod() {
        startSleepingThread(Duration.ofMillis(100));
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

    @Test
    void forkJoinPoolStartup() {
      collectExecutionResults(testKitBuilder(SysFjPool.class))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    static class SysFjPool extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        ForkJoinPool.commonPool().submit(() -> {}).join();
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    @DetectThreadLeaks.ExcludeThreads(ExcludeNamedAFilter.class)
    static class ExcludedByClassFilter extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread("excluded-a-1");
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
    @DetectThreadLeaks.ExcludeThreads(ExcludeNamedAFilter.class)
    static class NonExcludedStillFails extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread();
      }
    }

    @DetectThreadLeaks.ExcludeThreads(ExcludeNamedAFilter.class)
    static class Superclass extends IgnoreInStandaloneRuns {}

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    @DetectThreadLeaks.ExcludeThreads(ExcludeNamedBFilter.class)
    static class HierarchicalFilters extends Superclass {
      @Test
      void testMethod() {
        startSleepingThread("excluded-a-1");
        startSleepingThread("excluded-b-1");
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
        startThread(
                "bg-thread",
                () -> {
                  throw new RuntimeException("uncaught-test-exception");
                })
            .join();
      }
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
    static class UncaughtWithLeak extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startThread(
            "bg-thread",
            () -> {
              try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(1));
              } catch (InterruptedException ignored) {
                throw new RuntimeException("uncaught-test-exception");
              }
            });
      }
    }
  }

  @Nested
  class TestNoneScope {
    @Test
    void leakedThreadDoesNotFailWithNoneScope() {
      collectExecutionResults(testKitBuilder(NoneScope.class))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @DetectThreadLeaks(scope = DetectThreadLeaks.Scope.NONE)
    static class NoneScope extends IgnoreInStandaloneRuns {
      @Test
      void testMethod() {
        startSleepingThread();
      }
    }
  }

  /** Starts a thread that sleeps long enough to be observable as a leak. */
  private static void startSleepingThread() {
    startSleepingThread(Duration.ofMinutes(1));
  }

  /** Starts a named thread that sleeps long enough to be observable as a leak. */
  private static void startSleepingThread(String name) {
    startThread(
        name,
        () -> {
          try {
            Thread.sleep(Duration.ofMinutes(1));
          } catch (InterruptedException ignored) {
          }
        });
  }

  private static void startSleepingThread(Duration duration) {
    startThread(
        "sleeping-thread",
        () -> {
          try {
            Thread.sleep(duration.toMillis());
          } catch (InterruptedException ignored) {
          }
        });
  }

  private static Thread startThread(String name, Runnable r) {
    var t = new Thread(r, name);
    forkedThreads.add(t);
    t.start();
    return t;
  }
}
