package com.carrotsearch.randomizedtesting.jupiter.experiments;

import com.carrotsearch.randomizedtesting.jupiter.Randomized;
import com.carrotsearch.randomizedtesting.jupiter.RandomizedContext;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.RepeatedTest;

public class AdHoc {
  @Disabled
  @Randomized
  public static class TestClass {
    @RepeatedTest(10)
    public void testMethod(RandomizedContext ctx) {
      assert ctx.getRandom().nextBoolean();
    }
  }
}
