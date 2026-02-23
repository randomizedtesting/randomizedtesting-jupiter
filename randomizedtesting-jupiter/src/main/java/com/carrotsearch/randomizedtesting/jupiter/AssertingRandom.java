package com.carrotsearch.randomizedtesting.jupiter;

import java.util.Locale;
import java.util.Objects;
import java.util.Random;

/**
 * A {@link Random} with a delegate, preventing {@link Random#setSeed(long)} and locked to only be
 * usable by a single {@link Thread}.
 */
final class AssertingRandom extends Random {
  private final Random delegate;
  private final Thread ownerRef;
  private final String ownerName;
  private final StackTraceElement[] allocationStack;

  /**
   * Track out-of-context use of this {@link Random} instance. This introduces memory barriers and
   * scheduling side effects but there's no other way to do it in any other way and sharing randoms
   * across threads or test cases is very bad and worth tracking.
   */
  private volatile boolean valid = true;

  /**
   * Creates an instance to be used by <code>owner</code> thread and delegating to <code>delegate
   * </code> until {@link #destroy()}ed.
   */
  public AssertingRandom(Thread owner, Random delegate) {
    // Must be here, the only Random constructor. Has side effects on setSeed, see below.
    super(0);

    this.delegate = delegate;
    this.ownerRef = Objects.requireNonNull(owner);
    this.ownerName = owner.toString();
    this.allocationStack = Thread.currentThread().getStackTrace();
  }

  @Override
  protected int next(int bits) {
    throw new RuntimeException("Shouldn't be reachable.");
  }

  @Override
  public boolean nextBoolean() {
    checkValid();
    return delegate.nextBoolean();
  }

  @Override
  public void nextBytes(byte[] bytes) {
    checkValid();
    delegate.nextBytes(bytes);
  }

  @Override
  public double nextDouble() {
    checkValid();
    return delegate.nextDouble();
  }

  @Override
  public float nextFloat() {
    checkValid();
    return delegate.nextFloat();
  }

  @Override
  public double nextGaussian() {
    checkValid();
    return delegate.nextGaussian();
  }

  @Override
  public int nextInt() {
    checkValid();
    return delegate.nextInt();
  }

  @Override
  public int nextInt(int n) {
    checkValid();
    return delegate.nextInt(n);
  }

  @Override
  public long nextLong() {
    checkValid();
    return delegate.nextLong();
  }

  @Override
  public void setSeed(long seed) {
    // This is an interesting case of observing uninitialized object from an instance method
    // (this method is called from the superclass constructor).
    if (seed == 0 && delegate == null) {
      return;
    }

    throw noSetSeed();
  }

  @Override
  public String toString() {
    checkValid();
    return delegate.toString();
  }

  @Override
  public boolean equals(Object obj) {
    checkValid();
    return delegate.equals(obj);
  }

  @Override
  public int hashCode() {
    checkValid();
    return delegate.hashCode();
  }

  /** This object will no longer be usable after this method is called. */
  void destroy() {
    this.valid = false;
  }

  private static final class StackTraceHolder extends Throwable {
    public StackTraceHolder(String message) {
      super(message);
    }
  }

  /* */
  private void checkValid() {
    if (!valid) {
      throw new RuntimeException(
          "This Random instance has been invalidated and "
              + "is probably used out of its allowed context (test or suite).");
    }

    if (Thread.currentThread() != ownerRef) {
      Throwable allocationEx =
          new StackTraceHolder(
              "Original allocation stack for this Random (" + "allocated by " + ownerName + ")");
      allocationEx.setStackTrace(allocationStack);
      throw new RuntimeException(
          String.format(
              Locale.ROOT,
              "This Random instance is tied to thread %s, can't access it from thread: %s "
                  + "(Random instances must not be shared). Allocation stack is included as a nested exception.",
              ownerName,
              Thread.currentThread()),
          allocationEx);
    }
  }

  @Override
  protected Object clone() throws CloneNotSupportedException {
    checkValid();
    throw new CloneNotSupportedException("Don't clone test Randoms.");
  }

  static RuntimeException noSetSeed() {
    return new RuntimeException(
        "Changing the seed of Random instances is forbidden, it breaks repeatability"
            + " of tests. If you need a mutable instance of Random, create a new (local) instance,"
            + " preferably with the initial seed acquired from this Random instance.");
  }
}
