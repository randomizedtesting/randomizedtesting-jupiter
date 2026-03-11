package com.carrotsearch.randomizedtesting.jupiter;

import java.util.function.Predicate;
import java.util.regex.Pattern;

/**
 * @see DetectThreadLeaks.ExcludeThreads
 */
public class SystemThreadFilter implements Predicate<Thread> {
  private static final Pattern KNOWN_SUBSTRINGS =
      Pattern.compile("(^ForkJoinPool\\.)|(Poller SunPKCS11)");

  @Override
  public boolean test(Thread t) {
    ThreadGroup tgroup = t.getThreadGroup();

    // Ignore the entire system thread group.
    if (tgroup != null && "system".equals(tgroup.getName()) && tgroup.getParent() == null) {
      return true;
    }

    // These are some of the "known" threads that should be ignored.
    var tName = t.getName();
    return switch (tName) {
      case "JFR request timer",
          "YJPAgent-Telemetry",
          "MemoryPoolMXBean notification dispatcher",
          "AWT-AppKit",
          "process reaper",
          "JUnit5-serializer-daemon" ->
          true;
      default -> KNOWN_SUBSTRINGS.matcher(tName).find();
    };
  }
}
