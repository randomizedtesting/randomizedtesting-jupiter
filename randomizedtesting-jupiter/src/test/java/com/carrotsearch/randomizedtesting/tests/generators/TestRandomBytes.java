package com.carrotsearch.randomizedtesting.tests.generators;

import static com.carrotsearch.randomizedtesting.jupiter.generators.RandomBytes.randomBytesOfLength;
import static com.carrotsearch.randomizedtesting.jupiter.generators.RandomBytes.randomBytesOfLengthBetween;
import static com.carrotsearch.randomizedtesting.jupiter.generators.RandomNumbers.randomIntInRange;

import com.carrotsearch.randomizedtesting.jupiter.Randomized;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;

@Randomized
class TestRandomBytes {
  @RepeatedTest(100)
  void testRandomBytes(Random rnd) {
    int len = randomIntInRange(rnd, 0, 100);
    Assertions.assertThat(randomBytesOfLength(rnd, len)).hasSize(len);
  }

  @RepeatedTest(100)
  void testRandomBytesOfLength(Random rnd) {
    int min = randomIntInRange(rnd, 0, 100);
    int max = min + randomIntInRange(rnd, 0, 10);

    byte[] bytes = randomBytesOfLengthBetween(rnd, min, max);
    Assertions.assertThat(bytes.length).isBetween(min, max);
  }
}
