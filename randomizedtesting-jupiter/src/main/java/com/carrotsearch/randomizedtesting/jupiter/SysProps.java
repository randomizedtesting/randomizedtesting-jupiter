package com.carrotsearch.randomizedtesting.jupiter;

import java.util.Random;

/** System properties controlling the runtime behavior of this project extensions. */
public enum SysProps {
  /** Initial root seed value. If empty, a random value is picked for the root seed. */
  TESTS_SEED("tests.seed"),

  /**
   * String name of the factory used to create {@link Random} instances (see {@link
   * RandomInstanceFactory} for named implementations).
   *
   * @see RandomInstanceFactory
   */
  TESTS_RANDOM_FACTORY("tests.random.factory"),

  /**
   * A boolean property that enables stricter sanity assertions (including forbidding thread-shared
   * access to the returned {@link Random} instances, which makes tests more predictable).
   */
  TESTS_RANDOM_ASSERTING("tests.random.asserting"),

  /**
   * A "multiplier" for certain methods that return random values in {@link RandomizedTest}.
   *
   * @see RandomizedTest#multiplier()
   */
  TESTS_MULTIPLIER("tests.multiplier");

  public final String propertyKey;

  SysProps(String key) {
    this.propertyKey = key;
  }
}
