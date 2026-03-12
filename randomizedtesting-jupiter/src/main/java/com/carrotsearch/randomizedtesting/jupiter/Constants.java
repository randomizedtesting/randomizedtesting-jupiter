package com.carrotsearch.randomizedtesting.jupiter;

public final class Constants {
  private Constants() {}

  /**
   * Synthetic class used to augment stack traces and expose the seed in exceptions thrown while
   * running randomized tests.
   */
  public static final String AUGMENTED_SEED_CLASS = "__randomizedtesting.SeedChain";
}
