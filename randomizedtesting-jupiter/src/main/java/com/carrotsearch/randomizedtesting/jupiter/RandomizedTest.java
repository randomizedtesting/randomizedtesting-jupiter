package com.carrotsearch.randomizedtesting.jupiter;

import com.carrotsearch.randomizedtesting.jupiter.generators.RandomBytes;
import com.carrotsearch.randomizedtesting.jupiter.generators.RandomNumbers;
import com.carrotsearch.randomizedtesting.jupiter.generators.RandomPicks;
import com.carrotsearch.randomizedtesting.jupiter.generators.RandomStrings;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Randomized
@DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
@Execution(value = ExecutionMode.SAME_THREAD, reason = "Backward compatibility.")
@ExtendWith(RandomizedTest.SetMultiplier.class)
public abstract class RandomizedTest {
  private static Double multiplier;

  static final class SetMultiplier implements Extension, BeforeAllCallback, AfterAllCallback {
    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
      multiplier =
          extensionContext
              .getConfigurationParameter(SysProps.TESTS_RANDOM_FACTORY.propertyKey)
              .map(Double::parseDouble)
              .orElse(1.);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
      multiplier = null;
    }
  }

  // The following utility methods are declared for convenience and ease
  // of porting from the previous version of the framework.

  public static byte[] randomBytesOfLength(Random rnd, int length) {
    return RandomBytes.randomBytesOfLength(new Random(rnd.nextLong()), length);
  }

  public static byte[] randomBytesOfLength(Random rnd, int minLength, int maxLength) {
    return RandomBytes.randomBytesOfLengthBetween(new Random(rnd.nextLong()), minLength, maxLength);
  }

  /** Use {@link #randomIntInRange(Random, int, int)}. */
  @Deprecated(forRemoval = true)
  public static int randomIntBetween(Random rnd, int min, int max) {
    return randomIntInRange(rnd, min, max);
  }

  public static int randomIntInRange(Random rnd, int min, int max) {
    return RandomNumbers.randomIntInRange(rnd, min, max);
  }

  /** Use {@link #randomLongInRange(Random, long, long)}. */
  @Deprecated(forRemoval = true)
  public static long randomLongBetween(Random rnd, long min, long max) {
    return randomLongInRange(rnd, min, max);
  }

  public static long randomLongInRange(Random rnd, long min, long max) {
    return RandomNumbers.randomLongInRange(rnd, min, max);
  }

  /**
   * Returns a random value greater or equal to <code>min</code>. The value picked is affected
   * {@link #multiplier()}.
   *
   * @see #scaledRandomIntBetween(Random, int, int)
   */
  public static int atLeast(Random rnd, int min) {
    if (min < 0)
      throw new IllegalArgumentException("atLeast requires non-negative argument: " + min);
    return scaledRandomIntBetween(rnd, min, Integer.MAX_VALUE);
  }

  /**
   * Returns a non-negative random value smaller or equal <code>max</code>. The value picked is
   * affected by {@link #multiplier()}.
   *
   * <p>This method is effectively an alias to:
   *
   * <pre>
   * scaledRandomIntBetween(0, max)
   * </pre>
   *
   * @see #scaledRandomIntBetween(Random, int, int)
   */
  public static int atMost(Random rnd, int max) {
    if (max < 0)
      throw new IllegalArgumentException("atMost requires non-negative argument: " + max);
    return scaledRandomIntBetween(rnd, 0, max);
  }

  /** Rarely returns <code>true</code> in about 10% of all calls. */
  public static boolean rarely(Random rnd) {
    return rnd.nextInt(100) >= 90;
  }

  /** The exact opposite of {@link #rarely(Random)}. */
  public static boolean frequently(Random rnd) {
    return !rarely(rnd);
  }

  public static <T> T randomFrom(Random rnd, T[] array) {
    return RandomPicks.randomFrom(rnd, array);
  }

  public static <T> T randomFrom(Random rnd, List<T> list) {
    return RandomPicks.randomFrom(rnd, list);
  }

  public static byte randomFrom(Random rnd, byte[] array) {
    return RandomPicks.randomFrom(rnd, array);
  }

  public static short randomFrom(Random rnd, short[] array) {
    return RandomPicks.randomFrom(rnd, array);
  }

  public static int randomFrom(Random rnd, int[] array) {
    return RandomPicks.randomFrom(rnd, array);
  }

  public static char randomFrom(Random rnd, char[] array) {
    return RandomPicks.randomFrom(rnd, array);
  }

  public static float randomFrom(Random rnd, float[] array) {
    return RandomPicks.randomFrom(rnd, array);
  }

  public static long randomFrom(Random rnd, long[] array) {
    return RandomPicks.randomFrom(rnd, array);
  }

  public static double randomFrom(Random rnd, double[] array) {
    return RandomPicks.randomFrom(rnd, array);
  }

  //
  // "multiplied" or scaled value pickers. These will be affected by global multiplier.
  //

  /**
   * A multiplier can be used to linearly scale certain values. It can be used to make data or
   * iterations of certain tests "heavier" for nightly runs, for example.
   *
   * <p>The default multiplier value is 1.
   *
   * @see SysProps#TESTS_MULTIPLIER
   */
  public static double multiplier() {
    if (multiplier == null) {
      throw new RuntimeException("Multiplier not set?");
    }
    return multiplier;
  }

  /**
   * Returns the "scaled" number of iterations for loops which can have a variable iteration count.
   * This method is effectively an alias to {@link #scaledRandomIntBetween(Random, int, int)}.
   *
   * @param min minimum number of iterations (inclusive).
   * @param max maximum number of iterations (inclusive).
   */
  public static int iterations(Random rnd, int min, int max) {
    return scaledRandomIntBetween(rnd, min, max);
  }

  /**
   * Returns a "scaled" random number between min and max (inclusive). The number of iterations will
   * fall between [min, max], but the selection will also try to achieve the points below:
   *
   * <ul>
   *   <li>the multiplier can be used to move the number of iterations closer to min (if it is
   *       smaller than 1) or closer to max (if it is larger than 1). Setting the multiplier to 0
   *       will always result in picking min.
   *   <li>on normal runs, the number will be closer to min than to max.
   *   <li>on nightly runs, the number will be closer to max than to min.
   * </ul>
   *
   * @see #multiplier()
   * @param min Minimum (inclusive).
   * @param max Maximum (inclusive).
   * @return Returns a random number between min and max.
   */
  public static int scaledRandomIntBetween(Random rnd, int min, int max) {
    if (min < 0) throw new IllegalArgumentException("min must be >= 0: " + min);
    if (min > max) throw new IllegalArgumentException("max must be >= min: " + min + ", " + max);

    double point = Math.min(1, Math.abs(rnd.nextGaussian()) * 0.3) * multiplier();
    double range = max - min;
    int scaled = (int) Math.round(Math.min(point * range, range));
    return min + scaled;
  }

  public static boolean randomBoolean(Random rnd) {
    return rnd.nextBoolean();
  }

  public static byte randomByte(Random rnd) {
    return (byte) rnd.nextInt();
  }

  public static short randomShort(Random rnd) {
    return (short) rnd.nextInt();
  }

  public static int randomInt(Random rnd) {
    return rnd.nextInt();
  }

  public static float randomFloat(Random rnd) {
    return rnd.nextFloat();
  }

  public static double randomDouble(Random rnd) {
    return rnd.nextDouble();
  }

  public static long randomLong(Random rnd) {
    return rnd.nextLong();
  }

  public static double randomGaussian(Random rnd) {
    return rnd.nextGaussian();
  }

  /**
   * Return a random Locale from the available locales on the system.
   *
   * <p>Warning: This test assumes the returned array of locales is repeatable from jvm execution to
   * jvm execution. It _may_ be different from jvm to jvm and as such, it can render tests execute
   * in a different way.
   */
  public static Locale randomLocale(Random rnd) {
    Locale[] availableLocales = Locale.getAvailableLocales();
    Arrays.sort(availableLocales, Comparator.comparing(Locale::toString));
    return randomFrom(rnd, availableLocales);
  }

  /**
   * Return a random TimeZone from the available timezones on the system.
   *
   * <p>Warning: This test assumes the returned array of time zones is repeatable from jvm execution
   * to jvm execution. It _may_ be different from jvm to jvm and as such, it can render tests
   * execute in a different way.
   */
  public static TimeZone randomTimeZone(Random rnd) {
    final String[] availableIDs = TimeZone.getAvailableIDs();
    Arrays.sort(availableIDs);
    return TimeZone.getTimeZone(randomFrom(rnd, availableIDs));
  }

  public static String randomAsciiLettersOfLengthBetween(
      Random rnd, int minLetters, int maxLetters) {
    return RandomStrings.randomAsciiLettersOfLengthBetween(rnd, minLetters, maxLetters);
  }

  public static String randomAsciiLettersOfLength(Random rnd, int codeUnits) {
    return RandomStrings.randomAsciiLettersOfLength(rnd, codeUnits);
  }

  public static String randomAsciiAlphanumOfLengthBetween(
      Random rnd, int minCodeUnits, int maxCodeUnits) {
    return RandomStrings.randomAsciiAlphanumOfLengthBetween(rnd, minCodeUnits, maxCodeUnits);
  }

  public static String randomAsciiAlphanumOfLength(Random rnd, int codeUnits) {
    return RandomStrings.randomAsciiAlphanumOfLength(rnd, codeUnits);
  }

  public static String randomUnicodeOfLengthBetween(
      Random rnd, int minCodeUnits, int maxCodeUnits) {
    return RandomStrings.randomUnicodeOfLengthBetween(rnd, minCodeUnits, maxCodeUnits);
  }

  public static String randomUnicodeOfLength(Random rnd, int codeUnits) {
    return RandomStrings.randomUnicodeOfLength(rnd, codeUnits);
  }

  public static String randomUnicodeOfCodepointLengthBetween(
      Random rnd, int minCodePoints, int maxCodePoints) {
    return RandomStrings.randomUnicodeOfCodepointLengthBetween(rnd, minCodePoints, maxCodePoints);
  }

  public static String randomUnicodeOfCodepointLength(Random rnd, int codePoints) {
    return RandomStrings.randomUnicodeOfCodepointLength(rnd, codePoints);
  }

  public static String randomRealisticUnicodeOfLengthBetween(
      Random rnd, int minCodeUnits, int maxCodeUnits) {
    return RandomStrings.randomRealisticUnicodeOfLengthBetween(rnd, minCodeUnits, maxCodeUnits);
  }

  public static String randomRealisticUnicodeOfLength(Random rnd, int codeUnits) {
    return RandomStrings.randomRealisticUnicodeOfLength(rnd, codeUnits);
  }

  public static String randomRealisticUnicodeOfCodepointLengthBetween(
      Random rnd, int minCodePoints, int maxCodePoints) {
    return RandomStrings.randomRealisticUnicodeOfCodepointLengthBetween(
        rnd, minCodePoints, maxCodePoints);
  }

  public static String randomRealisticUnicodeOfCodepointLength(Random rnd, int codePoints) {
    return RandomStrings.randomRealisticUnicodeOfCodepointLength(rnd, codePoints);
  }
}
