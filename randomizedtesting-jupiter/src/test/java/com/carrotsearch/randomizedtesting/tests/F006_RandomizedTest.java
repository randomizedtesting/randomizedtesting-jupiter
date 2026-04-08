package com.carrotsearch.randomizedtesting.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.carrotsearch.randomizedtesting.jupiter.RandomizedTest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Verify that {@link com.carrotsearch.randomizedtesting.jupiter.RandomizedTest} parent class works
 * as expected.
 */
public class F006_RandomizedTest {
  @Nested
  class UtilityMethods extends RandomizedTest {
    @Test
    public void testAtLeast(Random rnd) {
      assertEquals(atLeast(rnd, Integer.MAX_VALUE), Integer.MAX_VALUE);
      int v = randomIntInRange(rnd, 0, Integer.MAX_VALUE);
      for (int i = 0; i < 10000; i++) assertTrue(atLeast(rnd, v) >= v);
    }

    @Test
    public void testRandomIntInRange(Random rnd) {
      boolean[] array = new boolean[10];
      for (int i = 0; i < 10000; i++) array[randomIntInRange(rnd, 0, array.length - 1)] = true;

      for (boolean b : array) assertTrue(b);
    }

    @Test
    public void testAtMost(Random rnd) {
      assertEquals(atMost(rnd, 0), 0);
      int v = randomIntInRange(rnd, 0, Integer.MAX_VALUE);
      for (int i = 0; i < 10000; i++) assertTrue(atMost(rnd, v) <= v);
    }

    @Test
    public void testRandomIntBetweenBoundaryCases(Random rnd) {
      for (int i = 0; i < 10000; i++) {
        int j = randomIntInRange(rnd, 0, Integer.MAX_VALUE);
        assertTrue(j >= 0 && j <= Integer.MAX_VALUE);

        // This must fall in range, but nonetheless
        randomIntInRange(rnd, Integer.MIN_VALUE, Integer.MAX_VALUE);
      }
    }

    @Test
    public void testRandomFromArray(Random rnd) {
      try {
        randomFrom(rnd, new Object[] {});
        fail();
      } catch (IllegalArgumentException e) {
        // expected.
      }

      Integer[] ints = new Integer[10];
      for (int i = 0; i < ints.length; i++) ints[i] = i;

      for (int i = 0; i < 10000; i++) {
        Integer j = randomFrom(rnd, ints);
        if (j != null) {
          ints[j] = null;
        }
      }

      for (int i = 0; i < ints.length; i++) assertTrue(ints[i] == null);
    }

    @Test
    public void testRandomFromList(Random rnd) {
      try {
        randomFrom(rnd, List.of());
        fail();
      } catch (IllegalArgumentException e) {
        // expected.
      }

      List<Integer> ints = new ArrayList<>();
      for (int i = 0; i < 10; i++) ints.add(i);

      for (int i = 0; i < 10000; i++) {
        Integer j = randomFrom(rnd, ints);
        if (j != null) {
          ints.set(j, null);
        }
      }

      for (int i = 0; i < ints.size(); i++) assertTrue(ints.get(i) == null);
    }

    @Test
    public void testRandomLocale(Random rnd) {
      assertNotNull(randomLocale(rnd));
    }

    @Test
    public void testRandomTimeZone(Random rnd) throws Exception {
      try {
        final String[] availableIDs = TimeZone.getAvailableIDs();
        Arrays.sort(availableIDs);
        for (String id : availableIDs) {
          assertNotNull(id);
          if (TimeZone.getTimeZone(id) == null) {
            fail("getTimeZone null: " + id);
          }
        }
      } catch (Exception e) {
        System.out.println("Wtf.");
        e.printStackTrace();
        throw e;
      }

      assertNotNull(randomTimeZone(rnd));
    }

    @Test
    public void testIterations(Random rnd) {
      assertEquals(0, iterations(rnd, 0, 0));
      assertEquals(Integer.MAX_VALUE, iterations(rnd, Integer.MAX_VALUE, Integer.MAX_VALUE));

      for (int i = 0; i < iterations(rnd, 1, 1000); i++) {
        int j = iterations(rnd, 0, 100);
        assertTrue(j >= 0 && j <= 100);
      }
    }

    @Test
    public void testRarely(Random rnd) {
      int rarely = 0;
      int calls = 100000;
      for (int i = 0; i < calls; i++) {
        if (rarely(rnd)) rarely++;
      }

      double rf = rarely / (double) calls * 100;
      Assertions.assertThat(rf > 5 && rf < 15).as("rarely should be > 5% & < 15%: " + rf).isTrue();
    }
  }
}
