package com.carrotsearch.randomizedtesting.tests.generators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.carrotsearch.randomizedtesting.jupiter.generators.CodepointSetGenerator;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class TestCodepointSetGenerator {
  private static final int[] codepoints = {
    'a', 'b', 'c', 'd', 0xd7ff, 0xffff, 0x10000, 0x1D11E, 0x10FFFD,
  };

  private static final int[] surrogates = {
    0x10000, 0x1D11E, 0x10FFFD,
  };

  private static final String withSurrogates = new String(codepoints, 0, codepoints.length);

  @Nested
  class CodepointSetOnChars extends StringGeneratorTestBase {
    CodepointSetOnChars() {
      super(new CodepointSetGenerator(new char[] {'a', 'b', 'c', 'd', 0x100, 0xd7ff, 0xffff}));
    }

    @Test
    void testAllCharactersUsed(Random rnd) {
      char[] domain = "abcdefABCDEF".toCharArray();
      Set<Character> chars = new HashSet<>();
      for (char chr : domain) chars.add(chr);

      CodepointSetGenerator gen = new CodepointSetGenerator(domain);
      for (int i = 0; i < 1000000 && !chars.isEmpty(); i++) {
        for (char ch : gen.ofCodeUnitsLength(rnd, 100, 100).toCharArray()) {
          chars.remove(ch);
        }
      }

      assertThat(chars).isEmpty();
    }

    @Test
    void testSurrogatesInConstructor() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new CodepointSetGenerator(withSurrogates.toCharArray()));
    }
  }

  @Nested
  class CodepointSetOnCodePoints extends StringGeneratorTestBase {
    CodepointSetOnCodePoints() {
      super(new CodepointSetGenerator(withSurrogates));
    }

    @Test
    void testAllCharactersUsed(Random rnd) {
      char[] domain = "abcdefABCDEF".toCharArray();
      Set<Character> chars = new HashSet<>();
      for (char chr : domain) chars.add(chr);

      CodepointSetGenerator gen = new CodepointSetGenerator(new String(domain));
      for (int i = 0; i < 1000000 && !chars.isEmpty(); i++) {
        for (char ch : gen.ofCodeUnitsLength(rnd, 100, 100).toCharArray()) {
          chars.remove(ch);
        }
      }

      assertThat(chars).isEmpty();
    }
  }

  @Nested
  class CodepointSetOnSurrogatesOnly extends StringGeneratorTestBase {
    CodepointSetOnSurrogatesOnly() {
      super(new CodepointSetGenerator(new String(surrogates, 0, surrogates.length)));
    }

    @Test
    void testOddCodePoints(Random rnd) {
      assertThrows(IllegalArgumentException.class, () -> generator.ofCodeUnitsLength(rnd, 3, 3));
    }

    @Override
    protected int iterationFix(int i) {
      return i & ~1; // Even only.
    }
  }
}
