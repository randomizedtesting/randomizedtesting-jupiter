package com.carrotsearch.randomizedtesting.jupiter;

import java.util.Random;

/**
 * Randomization context, with its own {@link Random} and associated source {@link SeedChain}.
 *
 * <p>Instances of this class are injected into test methods and hooks automatically, when they are
 * a parameter of such methods.
 *
 * <p>Instances of randomized context (and in particular, the {@link Random} instances returned from
 * {@link #getRandom()}) must not be shared with other threads. To acquire a {@link Random} for
 * another thread, use {@link #splitRandom()} method or {@link #splitRandom(Seed)} to initialize the
 * returned random differently for different threads.
 */
public interface RandomizedContext {
  SeedChain getSeedChain();

  Seed getRootSeed();

  /**
   * @return Returns a {@link Random} instance attached to this context. The instance is also bound
   *     to the thread that created the context and must not be used by other threads.
   */
  Random getRandom();

  /**
   * @return A {@link Random} instance aimed for use by the calling thread. There are no guarantees:
   *     two calls from the same thread may return the same or different {@link Random} objects.
   */
  Random splitRandom();

  /**
   * @return A {@link Random} instance aimed for use by the calling thread and initialized with the
   *     provided seed. Two calls from the same thread will return two different {@link Random}
   *     objects, initialized in an identical way.
   */
  Random splitRandom(Seed seed);
}
