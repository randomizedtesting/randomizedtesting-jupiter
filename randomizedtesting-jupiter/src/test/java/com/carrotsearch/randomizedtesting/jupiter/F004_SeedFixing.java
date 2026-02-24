package com.carrotsearch.randomizedtesting.jupiter;

import static com.carrotsearch.randomizedtesting.jupiter.infra.TestInfra.*;
import static org.junit.platform.testkit.engine.EventConditions.*;

import com.carrotsearch.randomizedtesting.jupiter.infra.IgnoreInStandaloneRuns;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

/** Ensure there is a way to quickly "fix" the seed for the given method or class. */
public class F004_SeedFixing {
  @Nested
  class TestSeedAnnotation {
    @Test
    public void testSeedFixing() {
      collectExecutionResults(
              testKitBuilder(T1.class)
                  .configurationParameter(
                      RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey, "dead:beef:cafe"))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @Randomized
    static class T1 extends IgnoreInStandaloneRuns {
      @Test
      @FixSeed("babe")
      void simpleTest(RandomizedContext ctx) {
        Assertions.assertThat(ctx.getSeedChain().toString()).isEqualTo("[DEAD:BEEF:BABE]");
      }
    }

    @Test
    public void testClassSeedFixing() {
      collectExecutionResults(
              testKitBuilder(T2.class)
                  .configurationParameter(
                      RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey, "dead"))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @Randomized
    @FixSeed("babe")
    static class T2 extends IgnoreInStandaloneRuns {
      @Test
      void ta(RandomizedContext ctx) {
        Assertions.assertThat(ctx.getSeedChain().toString()).startsWith("[DEAD:BABE:");
      }

      @Test
      void tb(RandomizedContext ctx) {
        Assertions.assertThat(ctx.getSeedChain().toString()).startsWith("[DEAD:BABE:");
      }
    }

    @Randomized
    @FixSeed("babe:caca")
    static class T3 extends IgnoreInStandaloneRuns {
      @Test
      void ta(RandomizedContext ctx) {
        Assertions.assertThat(ctx.getSeedChain().toString()).startsWith("[DEAD:BABE:CACA]");
      }

      @Test
      void tb(RandomizedContext ctx) {
        Assertions.assertThat(ctx.getSeedChain().toString()).startsWith("[DEAD:BABE:CACA]");
      }
    }

    @Test
    public void testRepeatedTests() {
      collectExecutionResults(
              testKitBuilder(T4.class)
                  .configurationParameter(
                      RandomizedContextSupplier.SysProps.TESTS_SEED.propertyKey, "dead"))
          .results()
          .allEvents()
          .assertThatEvents()
          .doNotHave(event(finishedWithFailure()));
    }

    @Randomized
    static class T4 extends IgnoreInStandaloneRuns {
      @RepeatedTest(5)
      @FixSeed("babe")
      void simpleTest(RandomizedContext ctx) {
        Assertions.assertThat(ctx.getSeedChain().toString()).endsWith(":BABE]");
      }
    }
  }
}
