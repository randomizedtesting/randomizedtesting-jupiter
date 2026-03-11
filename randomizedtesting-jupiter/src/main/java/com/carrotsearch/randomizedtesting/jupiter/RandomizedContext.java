package com.carrotsearch.randomizedtesting.jupiter;

import java.util.Random;

public interface RandomizedContext {
  SeedChain getSeedChain();

  Seed getRootSeed();

  Random getRandom();
}
