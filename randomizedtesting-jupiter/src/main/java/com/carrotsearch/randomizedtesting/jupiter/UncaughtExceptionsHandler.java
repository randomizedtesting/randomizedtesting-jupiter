package com.carrotsearch.randomizedtesting.jupiter;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Collects uncaught exceptions from threads during test execution. */
class UncaughtExceptionsHandler implements Thread.UncaughtExceptionHandler {
  private static final Logger LOGGER = Logger.getLogger(UncaughtExceptionsHandler.class.getName());

  record UncaughtException(String threadName, Throwable error) {}

  private final Thread.UncaughtExceptionHandler previous;
  private final List<UncaughtException> exceptions = new ArrayList<>();
  private boolean reporting = true;

  UncaughtExceptionsHandler(Thread.UncaughtExceptionHandler previous) {
    this.previous = previous;
  }

  @Override
  public void uncaughtException(Thread t, Throwable e) {
    synchronized (exceptions) {
      if (reporting) {
        LOGGER.log(Level.SEVERE, "Uncaught exception in thread: " + Threads.threadName(t), e);
        exceptions.add(new UncaughtException(Threads.threadName(t), e));
      }
    }
    if (previous != null) previous.uncaughtException(t, e);
  }

  void stopReporting() {
    synchronized (exceptions) {
      reporting = false;
    }
  }

  void resumeReporting() {
    synchronized (exceptions) {
      reporting = true;
    }
  }

  List<UncaughtException> getAndClear() {
    synchronized (exceptions) {
      var copy = new ArrayList<>(exceptions);
      exceptions.clear();
      return copy;
    }
  }

  /** Restores the previous default uncaught exception handler. */
  void restore() {
    Thread.setDefaultUncaughtExceptionHandler(previous);
  }
}
