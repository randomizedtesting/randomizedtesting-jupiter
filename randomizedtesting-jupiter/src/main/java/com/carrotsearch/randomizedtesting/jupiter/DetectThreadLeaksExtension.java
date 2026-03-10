package com.carrotsearch.randomizedtesting.jupiter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
  private static final String SNAPSHOT_KEY = "snapshot";
  private static final String CONCURRENT_KEY = "concurrent";

  /** Total time budget (ms) to join interrupted threads before giving up. */
  private static final long INTERRUPT_JOIN_MS = 2_000L;

  @Override
  public void beforeAll(ExtensionContext context) {
    if (context.getExecutionMode() != ExecutionMode.SAME_THREAD) {
      LOGGER.warning(
          "Thread leak detection is disabled: tests in ["
              + context.getDisplayName()
              + "] run in concurrent execution mode.");
      context.getStore(EXTENSION_NAMESPACE).put(CONCURRENT_KEY, Boolean.TRUE);
      return;
    }
    if (scope(context) == DetectThreadLeaks.Scope.SUITE) {
      context.getStore(EXTENSION_NAMESPACE).put(SNAPSHOT_KEY, liveThreads(buildFilter(context)));
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (isConcurrentMode(context) || scope(context) != DetectThreadLeaks.Scope.SUITE) {
      return;
    }
    checkLeaks(
        context.getStore(EXTENSION_NAMESPACE),
        "suite [" + context.getDisplayName() + "]",
        linger(context),
        buildFilter(context));
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    if (isConcurrentMode(context) || scope(context) != DetectThreadLeaks.Scope.TEST) {
      return;
    }
    context.getStore(EXTENSION_NAMESPACE).put(SNAPSHOT_KEY, liveThreads(buildFilter(context)));
  }

  @Override
  public void afterEach(ExtensionContext context) {
    if (isConcurrentMode(context) || scope(context) != DetectThreadLeaks.Scope.TEST) {
      return;
    }
    checkLeaks(
        context.getStore(EXTENSION_NAMESPACE),
        "test [" + context.getDisplayName() + "]",
        linger(context),
        buildFilter(context));
  }

  private static DetectThreadLeaks.Scope scope(ExtensionContext context) {
    return context.getRequiredTestClass().getAnnotation(DetectThreadLeaks.class).scope();
  }

  private static int linger(ExtensionContext context) {
    // Method-level annotation takes precedence over class-level.
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
   * (method → class → superclasses) and returns a combined predicate that excludes a thread when
   * any filter matches it.
   */
  private static Predicate<Thread> buildFilter(ExtensionContext context) {
    var filterClasses = new LinkedHashSet<Class<? extends Predicate<Thread>>>();

    context
        .getTestMethod()
        .ifPresent(
            m -> {
              var ann = m.getAnnotation(DetectThreadLeaks.ExcludeThreads.class);
              if (ann != null) {
                for (var c : ann.value()) filterClasses.add(c);
              }
            });

    for (Class<?> cls = context.getRequiredTestClass(); cls != null; cls = cls.getSuperclass()) {
      var ann = cls.getAnnotation(DetectThreadLeaks.ExcludeThreads.class);
      if (ann != null) {
        filterClasses.addAll(Arrays.asList(ann.value()));
      }
    }

    if (filterClasses.isEmpty()) {
      return t -> false;
    }

    var predicates =
        filterClasses.stream()
            .map(
                cls -> {
                  try {
                    return (Predicate<Thread>) cls.getDeclaredConstructor().newInstance();
                  } catch (Exception e) {
                    throw new RuntimeException(
                        "Cannot instantiate thread filter: " + cls.getName(), e);
                  }
                })
            .toList();

    return t -> predicates.stream().anyMatch(p -> p.test(t));
  }

  private static boolean isConcurrentMode(ExtensionContext context) {
    // Check the concurrent flag stored in beforeAll (class-level context = parent of method ctx).
    return context
        .getParent()
        .map(
            p ->
                Boolean.TRUE.equals(
                    p.getStore(EXTENSION_NAMESPACE).get(CONCURRENT_KEY, Boolean.class)))
        .orElse(false);
  }

  private static void checkLeaks(
      ExtensionContext.Store store, String description, int lingerMs, Predicate<Thread> filter) {
    var snapshot = store.get(SNAPSHOT_KEY, HashSet.class);
    if (snapshot == null) return;

    var leaked = leakedSince(snapshot, filter);
    if (leaked.isEmpty()) return;

    // Linger: poll until threads self-terminate or the window expires.
    if (lingerMs > 0) {
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
      if (leaked.isEmpty()) return;
    }

    // Interrupt leaked threads for cleanup, then wait briefly for them to terminate.
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

    // Report failure with stack traces captured before the interrupt.
    var sb = new StringBuilder(leaked.size() + " thread(s) leaked from " + description + ":");
    int cnt = 1;
    for (var entry : leaked.entrySet()) {
      sb.append(String.format("%n  %2d) %s", cnt++, Threads.threadName(entry.getKey())));
      for (var ste : entry.getValue()) {
        sb.append(String.format("%n        at %s", ste));
      }
    }
    throw new AssertionError(sb.toString());
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
