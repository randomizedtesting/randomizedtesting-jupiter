package com.carrotsearch.randomizedtesting.tests.generators;

import static com.carrotsearch.randomizedtesting.jupiter.generators.RandomNumbers.randomIntInRange;

import com.carrotsearch.randomizedtesting.jupiter.Randomized;
import com.carrotsearch.randomizedtesting.jupiter.generators.StringGenerator;
import java.util.Random;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/** Base class for testing {@link StringGenerator}s. */
@Randomized
abstract class StringGeneratorTestBase {
  protected final StringGenerator generator;

  protected StringGeneratorTestBase(StringGenerator generator) {
    this.generator = generator;
  }

  @RepeatedTest(10)
  public void checkFixedCodePointLength(Random rnd) {
    int codepoints = iterationFix(randomIntInRange(rnd, 1, 100));
    String s = generator.ofCodePointsLength(rnd, codepoints, codepoints);
    Assertions.assertThat(s.codePointCount(0, s.length())).as(s).isEqualTo(codepoints);
  }

  @RepeatedTest(10)
  public void checkRandomCodePointLength(Random rnd) {
    int from = iterationFix(randomIntInRange(rnd, 1, 100));
    int to = from + randomIntInRange(rnd, 0, 100);

    String s = generator.ofCodePointsLength(rnd, from, to);
    int codepoints = s.codePointCount(0, s.length());
    Assertions.assertThat(codepoints).isBetween(from, to);
  }

  @RepeatedTest(10)
  public void checkFixedCodeUnitLength(Random rnd) {
    int codeunits = iterationFix(randomIntInRange(rnd, 1, 100));
    String s = generator.ofCodeUnitsLength(rnd, codeunits, codeunits);
    Assertions.assertThat(s.length()).isEqualTo(codeunits);
    Assertions.assertThat(s.toCharArray()).hasSize(codeunits);
  }

  @RepeatedTest(10)
  public void checkRandomCodeUnitLength(Random rnd) {
    int from = iterationFix(randomIntInRange(rnd, 1, 100));
    int to = from + randomIntInRange(rnd, 0, 100);

    String s = generator.ofCodeUnitsLength(rnd, from, to);
    int codeunits = s.length();
    Assertions.assertThat(codeunits).isBetween(from, to);
  }

  @Test
  public void checkZeroLength(Random rnd) {
    Assertions.assertThat(generator.ofCodePointsLength(rnd, 0, 0)).isEmpty();
    Assertions.assertThat(generator.ofCodeUnitsLength(rnd, 0, 0)).isEmpty();
  }

  /** Correct the count if a given generator doesn't support all possible values (in tests). */
  protected int iterationFix(int i) {
    return i;
  }
}
