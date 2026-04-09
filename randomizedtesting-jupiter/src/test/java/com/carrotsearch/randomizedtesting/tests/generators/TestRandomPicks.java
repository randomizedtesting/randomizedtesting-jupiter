package com.carrotsearch.randomizedtesting.tests.generators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.carrotsearch.randomizedtesting.jupiter.Randomized;
import com.carrotsearch.randomizedtesting.jupiter.generators.RandomPicks;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

@Randomized
class TestRandomPicks {
  @Test
  void testRandomFromEmptyCollection(Random rnd) {
    assertThrows(
        IllegalArgumentException.class, () -> RandomPicks.randomFrom(rnd, new HashSet<>()));
  }

  @Test
  void testRandomFromCollection(Random rnd) {
    Object t = new Object();
    Object r = RandomPicks.randomFrom(rnd, new HashSet<>(List.of(t)));
    assertThat(r).isSameAs(t);
  }

  @Test
  void testRandomFromList(Random rnd) {
    assertThrows(
        IllegalArgumentException.class, () -> RandomPicks.randomFrom(rnd, new ArrayList<>()));
  }

  @Test
  void testRandomFromArray(Random rnd) {
    assertThrows(
        IllegalArgumentException.class, () -> RandomPicks.randomFrom(rnd, new Object[] {}));
  }

  @Test
  void testPicksActuallyDoPickRandomElements(Random rnd) {
    var source = IntStream.range(0, 1000).boxed().toList();

    // check randomFrom relies on the random state only.
    {
      var seed = rnd.nextLong();
      Assertions.assertThat(RandomPicks.randomFrom(new Random(seed), source))
          .isEqualTo(RandomPicks.randomFrom(new Random(seed), source));
    }

    // check that the elements picked are truly random.
    Assertions.assertThat(
            IntStream.range(0, 10)
                .mapToObj(iter -> RandomPicks.randomFrom(rnd, source))
                .collect(Collectors.toSet()))
        .hasSizeBetween(8, 10);
  }
}
