package com.carrotsearch.randomizedtesting.jupiter;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * The default filter containing sane defaults excluding system and ignorable threads when {@link
 * DetectThreadLeaks} extension is used.
 *
 * @see DetectThreadLeaks.ExcludeThreads
 */
public class SystemThreadFilter implements Predicate<Thread> {
  private static final Pattern KNOWN_SUBSTRINGS =
      Pattern.compile("(^ForkJoinPool\\.commonPool)|(Poller SunPKCS11)");

  @Override
  public boolean test(Thread t) {
    ThreadGroup tgroup = t.getThreadGroup();

    // Ignore the entire system thread group.
    if (tgroup != null && "system".equals(tgroup.getName()) && tgroup.getParent() == null) {
      return true;
    }

    var tName = t.getName();

    // Intellij Idea attaches to forked test processes using jmx/rmi. This is asynchronous and
    // unpredictable.
    if (Boolean.getBoolean("intellij.debug.agent")) {
      // JMX server connection timeout
      if (tName.startsWith("JMX server connection timeout") || tName.startsWith("RMI TCP")) {
        return true;
      }
    }

    // These are some of the "known" threads that should be ignored.
    return switch (tName) {
      case "JFR request timer",
          "YJPAgent-Telemetry",
          "MemoryPoolMXBean notification dispatcher",
          "AWT-AppKit",
          "process reaper",
          // CompletableFuture's delayed executor thread on JDK < 25; JDK 25+ uses
          // "ForkJoinPool.commonPool-delayScheduler", covered by KNOWN_SUBSTRINGS.
          "CompletableFutureDelayScheduler",
          "JUnit5-serializer-daemon" ->
          true;
      default -> KNOWN_SUBSTRINGS.matcher(tName).find();
    };
  }
}
