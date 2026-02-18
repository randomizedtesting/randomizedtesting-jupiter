package com.carrotsearch.randomizedtesting.junitframework;

import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

record SeedChain(List<Seed> seeds) {
  private static final SeedChain EMPTY = new SeedChain(List.of());

  static SeedChain parse(String chain) {
    return new SeedChain(
        Stream.of(chain.replaceAll("[\\[\\]]", "").split("[:]"))
            .map(v -> v.trim().toLowerCase(Locale.ROOT))
            .map(
                v -> {
                  if (v.isEmpty() || v.equals("*")) {
                    return Seed.UNSPECIFIED;
                  } else if (!v.matches("[0-9A-Fa-f]+")) {
                    throw new IllegalArgumentException(
                        "Invalid component \"" + v + "\" in seed chain: " + chain);
                  }
                  return new Seed(Long.parseUnsignedLong(v, 16));
                })
            .toList());
  }

  @Override
  public String toString() {
    return "["
        + seeds.stream()
            .map(v -> v == Seed.UNSPECIFIED ? "*" : v.toString())
            .collect(Collectors.joining(":"))
        + "]";
  }

  record FirstAndRest(Seed first, SeedChain rest) {}

  public FirstAndRest pop() {
    if (seeds.isEmpty()) {
      return new FirstAndRest(Seed.UNSPECIFIED, SeedChain.EMPTY);
    }

    var first = seeds.iterator().next();
    var rest = new SeedChain(seeds.subList(1, seeds.size()));
    return new FirstAndRest(first, rest);
  }

  private long nextRandomValue() {
    return new Random().nextLong();
  }
}
