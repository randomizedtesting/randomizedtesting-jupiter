package com.carrotsearch.randomizedtesting.jupiter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class RandomizedContext {
  private final RandomizedContext parent;
  private final Seed seed;
  final String contextId;

  private final SeedChain remainingSeedChain;

  private final Random random;
  private final RandomFactory randomFactory;

  RandomizedContext(
      String contextId,
      RandomizedContext parent,
      RandomFactory randomFactory,
      Seed seed,
      SeedChain remainingSeedChain) {
    this.contextId = contextId;
    this.parent = parent;
    this.remainingSeedChain = remainingSeedChain;
    this.randomFactory = randomFactory;

    assert !seed.isUnspecified();
    this.seed = seed;
    this.random = randomFactory.apply(seed.value());
  }

  @Override
  public String toString() {
    return "Randomized context [" + ("seedChain=" + getSeedChain() + ",") + "]";
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
    return random;
  }

  RandomizedContext deriveNew(ExtensionContext extensionContext) {
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
        extensionContext.getUniqueId(), this, randomFactory, nextSeed, firstAndRest.rest());
  }
}
