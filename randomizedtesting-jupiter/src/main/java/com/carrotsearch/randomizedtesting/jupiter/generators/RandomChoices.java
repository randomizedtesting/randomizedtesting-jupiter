package com.carrotsearch.randomizedtesting.jupiter.generators;

import java.util.Random;

/** Random selections of objects. */
public final class RandomChoices {
  /** Returns {@code true} rarely (~10% of calls). */
  public static boolean rarely(Random r) {
    return r.nextInt(100) >= 90;
  }
}
