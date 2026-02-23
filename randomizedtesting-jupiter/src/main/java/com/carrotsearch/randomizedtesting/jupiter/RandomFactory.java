package com.carrotsearch.randomizedtesting.jupiter;

import java.util.Random;
import java.util.function.LongFunction;

/** Supplier of {@link Random} instances, given the initial seed value. */
public interface RandomFactory extends LongFunction<Random> {}
