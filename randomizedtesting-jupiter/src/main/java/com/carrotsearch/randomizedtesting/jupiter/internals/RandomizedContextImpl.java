package com.carrotsearch.randomizedtesting.jupiter.internals;

import com.carrotsearch.randomizedtesting.jupiter.FixSeed;
import com.carrotsearch.randomizedtesting.jupiter.Hashing;
import com.carrotsearch.randomizedtesting.jupiter.RandomizedContext;
import com.carrotsearch.randomizedtesting.jupiter.Seed;
import com.carrotsearch.randomizedtesting.jupiter.SeedChain;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class RandomizedContextImpl implements Closeable, RandomizedContext {
  private final RandomizedContextImpl parent;
  private final Seed seed;
  final String contextId;

  private final SeedChain remainingSeedChain;

  private final Random random;
  private final RandomFactory randomFactory;

  RandomizedContextImpl(
      String contextId,
      RandomizedContextImpl parent,
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

  @Override
  public SeedChain getSeedChain() {
    ArrayList<Seed> seeds = new ArrayList<>();
    for (RandomizedContextImpl c = this; c != null; c = c.getParent()) {
      seeds.add(c.seed);
    }
    Collections.reverse(seeds);
    return new SeedChain(seeds);
  }

  private RandomizedContextImpl getParent() {
    return parent;
  }

  /**
   * @return Returns the root seed (randomization source).
   * @see RandomizedContextExtension.SysProps#TESTS_SEED
   */
  @Override
  public Seed getRootSeed() {
    return getSeedChain().seeds().getFirst();
  }

  @Override
  public Random getRandom() {
    return random;
  }

  RandomizedContextImpl deriveNew(ExtensionContext extensionContext) {
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

    SeedChain seedChain;
    var annotationSeed = extensionContext.getElement().map(e -> e.getAnnotation(FixSeed.class));
    if (annotationSeed.isPresent()) {
      seedChain = SeedChain.parse(annotationSeed.get().value());
      for (var seed : seedChain.seeds()) {
        if (seed.isUnspecified()) {
          throw new RuntimeException(
              String.format(
                  Locale.ROOT,
                  "@%s annotatoin must declare concrete seeds or seed chains on: %s",
                  FixSeed.class.getName(),
                  extensionContext.getElement().get()));
        }
      }
    } else {
      seedChain = this.remainingSeedChain;
    }

    var firstAndRest = FirstAndRest.from(seedChain);
    var nextSeed = firstAndRest.first();
    var remainingChain = firstAndRest.rest();
    if (nextSeed.isUnspecified()) {
      nextSeed = new Seed(this.seed.value() ^ Hashing.hash(extensionContext.getUniqueId()));
    }

    return new RandomizedContextImpl(
        extensionContext.getUniqueId(), this, randomFactory, nextSeed, remainingChain);
  }

  @Override
  public void close() throws IOException {
    if (random instanceof Closeable c) {
      c.close();
    }
  }
}
