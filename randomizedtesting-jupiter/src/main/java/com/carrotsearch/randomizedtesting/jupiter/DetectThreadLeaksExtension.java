package com.carrotsearch.randomizedtesting.jupiter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.ExecutionMode;

/** JUnit Jupiter extension implementing {@link DetectThreadLeaks}. */
public class DetectThreadLeaksExtension
    implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback {

  private static final Logger LOGGER = Logger.getLogger(DetectThreadLeaksExtension.class.getName());
  private static final ExtensionContext.Namespace EXTENSION_NAMESPACE =
      ExtensionContext.Namespace.create(DetectThreadLeaksExtension.class);
  private static final String THREAD_SNAPSHOT_KEY = "snapshot";
  private static final String CONCURRENT_KEY = "concurrent";
  private static final String UNCAUGHT_EXCEPTION_HANDLER_KEY = "uncaught-exception-handler";

  /** Total time budget (ms) to join interrupted threads before giving up. */
  private static final long INTERRUPT_JOIN_MS = 2_000L;

  @Override
  public void beforeAll(ExtensionContext context) {
    if (scope(context) == DetectThreadLeaks.Scope.NONE) {
      return;
    }

    if (context.getExecutionMode() != ExecutionMode.SAME_THREAD) {
      LOGGER.warning(
          "Thread leak detection is disabled: tests in ["
              + context.getDisplayName()
              + "] run in concurrent execution mode.");
      context.getStore(EXTENSION_NAMESPACE).put(CONCURRENT_KEY, Boolean.TRUE);
      return;
    }

    var store = context.getStore(EXTENSION_NAMESPACE);
    var filter = buildFilter(context);
    store.put(UNCAUGHT_EXCEPTION_HANDLER_KEY, installUncaughtExceptionHandler());
    store.put(THREAD_SNAPSHOT_KEY, liveThreads(filter));
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    if (isConcurrentMode(context) || scope(context) != DetectThreadLeaks.Scope.TEST) {
      return;
    }

    var store = context.getStore(EXTENSION_NAMESPACE);
    var filter = buildFilter(context);
    store.put(THREAD_SNAPSHOT_KEY, liveThreads(filter));
  }

  @Override
  public void afterEach(ExtensionContext context) {
    if (isConcurrentMode(context) || scope(context) != DetectThreadLeaks.Scope.TEST) {
      return;
    }

    var store = context.getStore(EXTENSION_NAMESPACE);
    var handler = store.get(UNCAUGHT_EXCEPTION_HANDLER_KEY, UncaughtExceptionsHandler.class);
    try {
      checkLeaks(
          store,
          "test [" + context.getDisplayName() + "]",
          linger(context),
          buildFilter(context),
          handler);
    } finally {
      if (handler != null) handler.restore();
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (isConcurrentMode(context) || scope(context) == DetectThreadLeaks.Scope.NONE) {
      return;
    }

    var store = context.getStore(EXTENSION_NAMESPACE);
    var handler = store.get(UNCAUGHT_EXCEPTION_HANDLER_KEY, UncaughtExceptionsHandler.class);
    try {
      checkLeaks(
          store,
          "suite [" + context.getDisplayName() + "]",
          linger(context),
          buildFilter(context),
          handler);
    } finally {
      if (handler != null) handler.restore();
    }
  }

  private static UncaughtExceptionsHandler installUncaughtExceptionHandler() {
    var handler = new UncaughtExceptionsHandler(Thread.getDefaultUncaughtExceptionHandler());
    Thread.setDefaultUncaughtExceptionHandler(handler);
    return handler;
  }

  private static DetectThreadLeaks.Scope scope(ExtensionContext context) {
    return context.getRequiredTestClass().getAnnotation(DetectThreadLeaks.class).scope();
  }

  private static int linger(ExtensionContext context) {
    var methodAnn =
        context
            .getTestMethod()
            .map(m -> m.getAnnotation(DetectThreadLeaks.LingerTime.class))
            .orElse(null);
    if (methodAnn != null) return methodAnn.millis();

    var classAnn = context.getRequiredTestClass().getAnnotation(DetectThreadLeaks.LingerTime.class);
    return classAnn == null ? 0 : classAnn.millis();
  }

  /**
   * Collects {@link DetectThreadLeaks.ExcludeThreads} filter classes from the entire hierarchy
   * (method to class to superclasses) and returns a combined predicate that excludes a thread when
   * any filter matches it.
   */
  private static Predicate<Thread> buildFilter(ExtensionContext context) {
    var filterClasses = new LinkedHashSet<Predicate<Thread>>();

    for (Class<?> cls = context.getRequiredTestClass(); cls != null; cls = cls.getSuperclass()) {
      var ann = cls.getAnnotation(DetectThreadLeaks.ExcludeThreads.class);
      if (ann != null) {
        for (var c : ann.value()) {
          try {
            filterClasses.add(c.getDeclaredConstructor().newInstance());
          } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot instantiate thread filter: " + cls.getName(), e);
          }
        }
      }
    }

    if (filterClasses.isEmpty()) {
      return t -> false;
    }

    return t -> filterClasses.stream().anyMatch(p -> p.test(t));
  }

  private static boolean isConcurrentMode(ExtensionContext context) {
    return context
        .getParent()
        .map(
            p ->
                Boolean.TRUE.equals(
                    p.getStore(EXTENSION_NAMESPACE).get(CONCURRENT_KEY, Boolean.class)))
        .orElse(false);
  }

  private static void checkLeaks(
      ExtensionContext.Store store,
      String description,
      int lingerMs,
      Predicate<Thread> filter,
      UncaughtExceptionsHandler handler) {
    var snapshot = store.get(THREAD_SNAPSHOT_KEY, HashSet.class);
    AssertionError leakError = null;

    if (snapshot != null) {
      var leaked = leakedSince(snapshot, filter);

      // Linger: poll until threads self-terminate or the window expires.
      if (!leaked.isEmpty() && lingerMs > 0) {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(lingerMs);
        while (!leaked.isEmpty() && System.nanoTime() < deadline) {
          try {
            long remainingMs = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime());
            Thread.sleep(Math.max(1L, Math.min(100L, remainingMs)));
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            break;
          }
          leaked = leakedSince(snapshot, filter);
        }
      }

      if (!leaked.isEmpty()) {
        // Suppress uncaught exception reporting during the interrupt/join phase to avoid
        // capturing expected InterruptedException-related exceptions from cleaned-up threads.
        if (handler != null) {
          handler.stopReporting();
        }

        try {
          leaked.keySet().forEach(Thread::interrupt);
          long joinDeadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(INTERRUPT_JOIN_MS);
          for (Thread t : leaked.keySet()) {
            long remaining = TimeUnit.NANOSECONDS.toMillis(joinDeadline - System.nanoTime());
            if (remaining <= 0) break;
            try {
              t.join(remaining);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
              break;
            }
          }
        } finally {
          if (handler != null) {
            handler.resumeReporting();
          }
        }

        var sb = new StringBuilder(leaked.size() + " thread(s) leaked from " + description + ":");
        int cnt = 1;
        for (var entry : leaked.entrySet()) {
          sb.append(String.format("%n  %2d) %s", cnt++, Threads.threadName(entry.getKey())));
          for (var ste : entry.getValue()) {
            sb.append(String.format("%n        at %s", ste));
          }
        }
        leakError = new AssertionError(sb.toString());
      }
    }

    // Collect uncaught exceptions regardless of whether threads leaked.
    List<UncaughtExceptionsHandler.UncaughtException> uncaught =
        handler != null ? handler.getAndClear() : List.of();

    if (leakError == null && uncaught.isEmpty()) return;

    // Combine: leak error first (if any), uncaught exceptions after; all but the first
    // are attached as suppressed on the thrown error.
    var errors = new ArrayList<AssertionError>();
    if (leakError != null) errors.add(leakError);
    for (var ue : uncaught) {
      errors.add(
          new AssertionError("Uncaught exception in thread [" + ue.threadName() + "]", ue.error()));
    }
    var first = errors.get(0);
    errors.subList(1, errors.size()).forEach(first::addSuppressed);
    throw first;
  }

  private static Map<Thread, StackTraceElement[]> leakedSince(
      HashSet<?> snapshot, Predicate<Thread> filter) {
    var current = liveThreadsWithStacks(filter);
    current.keySet().removeAll(snapshot);
    return current;
  }

  private static HashSet<Thread> liveThreads(Predicate<Thread> filter) {
    return new HashSet<>(liveThreadsWithStacks(filter).keySet());
  }

  private static Map<Thread, StackTraceElement[]> liveThreadsWithStacks(Predicate<Thread> filter) {
    return Thread.getAllStackTraces().entrySet().stream()
        .filter(e -> e.getKey().isAlive())
        .filter(e -> !isKnownSystemThread(e.getKey()))
        .filter(e -> !filter.test(e.getKey()))
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  private static boolean isKnownSystemThread(Thread t) {
    ThreadGroup tgroup = t.getThreadGroup();

    if (tgroup != null && "system".equals(tgroup.getName()) && tgroup.getParent() == null) {
      return true;
    }

    return switch (t.getName()) {
      case "JFR request timer",
          "YJPAgent-Telemetry",
          "MemoryPoolMXBean notification dispatcher",
          "AWT-AppKit",
          "process reaper",
          "JUnit5-serializer-daemon" ->
          true;
      default -> t.getName().contains("Poller SunPKCS11");
    };
  }
}
