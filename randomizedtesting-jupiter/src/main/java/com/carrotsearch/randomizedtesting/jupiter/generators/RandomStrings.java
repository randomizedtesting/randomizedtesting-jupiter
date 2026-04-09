package com.carrotsearch.randomizedtesting.jupiter.generators;

import java.util.Random;

/** A facade to various implementations of {@link StringGenerator} interface. */
public final class RandomStrings {
  public static final RealisticUnicodeGenerator realisticUnicodeGenerator =
      new RealisticUnicodeGenerator();
  public static final UnicodeGenerator unicodeGenerator = new UnicodeGenerator();
  public static final AsciiLettersGenerator asciiLettersGenerator = new AsciiLettersGenerator();
  public static final AsciiAlphanumGenerator asciiAlphanumGenerator = new AsciiAlphanumGenerator();

  public static String randomAsciiLettersOfLengthBetween(
      Random r, int minCodeUnits, int maxCodeUnits) {
    return asciiLettersGenerator.ofCodeUnitsLength(r, minCodeUnits, maxCodeUnits);
  }

  public static String randomAsciiLettersOfLength(Random r, int codeUnits) {
    return asciiLettersGenerator.ofCodeUnitsLength(r, codeUnits, codeUnits);
  }

  public static String randomAsciiAlphanumOfLengthBetween(
      Random r, int minCodeUnits, int maxCodeUnits) {
    return asciiAlphanumGenerator.ofCodeUnitsLength(r, minCodeUnits, maxCodeUnits);
  }

  public static String randomAsciiAlphanumOfLength(Random r, int codeUnits) {
    return asciiAlphanumGenerator.ofCodeUnitsLength(r, codeUnits, codeUnits);
  }

  public static String randomUnicodeOfLengthBetween(Random r, int minCodeUnits, int maxCodeUnits) {
    return unicodeGenerator.ofCodeUnitsLength(r, minCodeUnits, maxCodeUnits);
  }

  public static String randomUnicodeOfLength(Random r, int codeUnits) {
    return unicodeGenerator.ofCodeUnitsLength(r, codeUnits, codeUnits);
  }

  public static String randomUnicodeOfCodepointLengthBetween(
      Random r, int minCodePoints, int maxCodePoints) {
    return unicodeGenerator.ofCodePointsLength(r, minCodePoints, maxCodePoints);
  }

  public static String randomUnicodeOfCodepointLength(Random r, int codePoints) {
    return unicodeGenerator.ofCodePointsLength(r, codePoints, codePoints);
  }

  public static String randomRealisticUnicodeOfLengthBetween(
      Random r, int minCodeUnits, int maxCodeUnits) {
    return realisticUnicodeGenerator.ofCodeUnitsLength(r, minCodeUnits, maxCodeUnits);
  }

  public static String randomRealisticUnicodeOfLength(Random r, int codeUnits) {
    return realisticUnicodeGenerator.ofCodeUnitsLength(r, codeUnits, codeUnits);
  }

  public static String randomRealisticUnicodeOfCodepointLengthBetween(
      Random r, int minCodePoints, int maxCodePoints) {
    return realisticUnicodeGenerator.ofCodePointsLength(r, minCodePoints, maxCodePoints);
  }

  public static String randomRealisticUnicodeOfCodepointLength(Random r, int codePoints) {
    return realisticUnicodeGenerator.ofCodePointsLength(r, codePoints, codePoints);
  }
}
