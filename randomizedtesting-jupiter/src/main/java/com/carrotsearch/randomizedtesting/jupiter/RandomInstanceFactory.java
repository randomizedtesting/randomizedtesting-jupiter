package com.carrotsearch.randomizedtesting.jupiter;

import com.carrotsearch.randomizedtesting.jupiter.internals.Xoroshiro128PlusRandom;
import java.util.Locale;
import java.util.Random;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** The supplier of {@link Random} instances. */
public enum RandomInstanceFactory implements Supplier<LongFunction<Random>> {
  /** The default, built-in {@link Random}. There may be some synchronization overhead. */
  JDK,

  /**
   * Xoroshiro128PlusRandom. Not synchronized anywhere.
   *
   * @see "https://prng.di.unimi.it/"
   */
  XOROSHIRO_128_PLUS;

  public static RandomInstanceFactory parse(String v) {
    try {
      return RandomInstanceFactory.valueOf(v.toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Can't parse "
              + SysProps.TESTS_RANDOM_FACTORY.propertyKey
              + " property: "
              + v
              + " [valid values: "
              + Stream.of(RandomInstanceFactory.values())
                  .map(vv -> vv.name().toLowerCase(Locale.ROOT))
                  .collect(Collectors.joining(", "))
              + "]");
    }
  }

  @Override
  public LongFunction<Random> get() {
    return switch (this) {
      case JDK -> Random::new;
      case XOROSHIRO_128_PLUS -> Xoroshiro128PlusRandom::new;
    };
  }
}
