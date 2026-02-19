package com.carrotsearch.randomizedtesting.jupiter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import java.util.function.LongFunction;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class RandomizedContext {
  private final RandomizedContext parent;
  private final Thread owner;
  private final Seed seed;
  final String contextId;

  private final SeedChain remainingSeedChain;

  private final Random random;
  private final LongFunction<Random> seedToRandomFn;

  RandomizedContext(
      String contextId,
      RandomizedContext parent,
      Thread owner,
      LongFunction<Random> seedToRandomFn,
      Seed seed,
      SeedChain remainingSeedChain) {
    this.contextId = contextId;
    this.parent = parent;
    this.owner = owner;
    this.remainingSeedChain = remainingSeedChain;
    this.seedToRandomFn = seedToRandomFn;

    assert !seed.isUnspecified();
    this.seed = seed;
    this.random = seedToRandomFn.apply(seed.value());
  }

  @Override
  public String toString() {
    return "Randomized context ["
        + ("seedChain=" + getSeedChain() + ",")
        + ("thread=" + Threads.threadName(owner))
        + "]";
  }

  SeedChain getSeedChain() {
    ArrayList<Seed> seeds = new ArrayList<>();
    for (RandomizedContext c = this; c != null; c = c.getParent()) {
      seeds.add(c.seed);
    }
    Collections.reverse(seeds);
    return new SeedChain(seeds);
  }

  /**
   * @return Returns the root seed (randomization source).
   * @see RandomizedContextSupplier.SysProps#TESTS_SEED
   */
  public Seed getRootSeed() {
    return getSeedChain().seeds().getFirst();
  }

  private RandomizedContext getParent() {
    return parent;
  }

  public Random getRandom() {
    if (Thread.currentThread() != owner) {
      throw new RuntimeException(
          String.format(
              Locale.ROOT,
              "This %s instance is bound to thread %s, can't access it from thread: %s",
              RandomizedContext.class.getName(),
              owner,
              Thread.currentThread()));
    }

    return random;
  }

  RandomizedContext deriveNew(Thread thread, ExtensionContext extensionContext) {
    // sanity check.
    {
      var id = extensionContext.getUniqueId();
      for (var ctx = this; ctx != null; ctx = ctx.getParent()) {
        if (Objects.equals(ctx.contextId, id)) {
          throw new RuntimeException(
              "deriveNew on a context that is already present in the parent chain: " + id);
        }
      }
    }

    var firstAndRest = this.remainingSeedChain.pop();
    var nextSeed = firstAndRest.first();
    if (nextSeed.isUnspecified()) {
      nextSeed = new Seed(this.seed.value() ^ Hashing.longHash(extensionContext.getUniqueId()));
    }

    return new RandomizedContext(
        extensionContext.getUniqueId(),
        this,
        thread,
        seedToRandomFn,
        nextSeed,
        firstAndRest.rest());
  }
}
