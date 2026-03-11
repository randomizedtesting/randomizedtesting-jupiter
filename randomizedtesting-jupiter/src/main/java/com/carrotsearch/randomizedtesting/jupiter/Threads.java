package com.carrotsearch.randomizedtesting.jupiter;

final class Threads {
  Threads() {}

  /** Collect thread information, JVM vendor insensitive. */
  static String threadName(Thread t) {
    return "Thread["
        + ("id=" + t.threadId())
        + (", name=" + t.getName())
        + (", state=" + t.getState())
        + (", group=" + groupName(t.getThreadGroup()))
        + "]";
  }

  private static String groupName(ThreadGroup threadGroup) {
    return threadGroup == null ? "{null group}" : threadGroup.getName();
  }
}
