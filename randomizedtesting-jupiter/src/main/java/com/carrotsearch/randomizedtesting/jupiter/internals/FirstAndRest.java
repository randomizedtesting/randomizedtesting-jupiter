package com.carrotsearch.randomizedtesting.jupiter.internals;

import com.carrotsearch.randomizedtesting.jupiter.Seed;
import com.carrotsearch.randomizedtesting.jupiter.SeedChain;
import java.util.List;

record FirstAndRest(Seed first, SeedChain rest) {
  private static final SeedChain EMPTY = new SeedChain(List.of());

  public static FirstAndRest from(SeedChain seeds) {
    return from(seeds.seeds());
  }

  public static FirstAndRest from(List<Seed> seeds) {
    if (seeds.isEmpty()) {
      return new FirstAndRest(Seed.UNSPECIFIED, EMPTY);
    }

    var first = seeds.iterator().next();
    var rest = new SeedChain(seeds.subList(1, seeds.size()));
    return new FirstAndRest(first, rest);
  }
}
