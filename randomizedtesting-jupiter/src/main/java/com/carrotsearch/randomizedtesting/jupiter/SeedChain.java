package com.carrotsearch.randomizedtesting.jupiter;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A seed chain determines randomization if {@link Randomized} extension is used. A seed chain is a
 * sequence of {@link Seed}s, typically associated with one or more hierarchical junit jupiter
 * contexts.
 */
public record SeedChain(List<Seed> seeds) {
  public static SeedChain parse(String chain) {
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
}
