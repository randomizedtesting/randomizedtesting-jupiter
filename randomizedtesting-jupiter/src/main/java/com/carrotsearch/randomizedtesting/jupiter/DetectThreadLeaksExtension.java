package com.carrotsearch.randomizedtesting.jupiter;

import java.util.HashSet;
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
      context.getStore(EXTENSION_NAMESPACE).put(SNAPSHOT_KEY, liveThreads());
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    if (isConcurrentMode(context) || scope(context) != DetectThreadLeaks.Scope.SUITE) {
      return;
    }
    checkLeaks(context.getStore(EXTENSION_NAMESPACE), "suite [" + context.getDisplayName() + "]");
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    if (isConcurrentMode(context) || scope(context) != DetectThreadLeaks.Scope.TEST) {
      return;
    }
    context.getStore(EXTENSION_NAMESPACE).put(SNAPSHOT_KEY, liveThreads());
  }

  @Override
  public void afterEach(ExtensionContext context) {
    if (isConcurrentMode(context) || scope(context) != DetectThreadLeaks.Scope.TEST) {
      return;
    }
    checkLeaks(context.getStore(EXTENSION_NAMESPACE), "test [" + context.getDisplayName() + "]");
  }

  private static DetectThreadLeaks.Scope scope(ExtensionContext context) {
    return context.getRequiredTestClass().getAnnotation(DetectThreadLeaks.class).scope();
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

  private static void checkLeaks(ExtensionContext.Store store, String description) {
    var snapshot = store.get(SNAPSHOT_KEY, HashSet.class);
    if (snapshot == null) return;

    var leaked = liveThreads();
    leaked.removeAll(snapshot);
    leaked.removeIf(t -> !t.isAlive());

    if (!leaked.isEmpty()) {
      var sb = new StringBuilder(leaked.size() + " thread(s) leaked from " + description + ":");
      leaked.forEach(t -> sb.append("\n  ").append(Threads.threadName(t)));
      throw new AssertionError(sb.toString());
    }
  }

  private static HashSet<Thread> liveThreads() {
    return Thread.getAllStackTraces().keySet().stream()
        .filter(Thread::isAlive)
        .filter(t -> !isKnownSystemThread(t))
        .collect(Collectors.toCollection(HashSet::new));
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
