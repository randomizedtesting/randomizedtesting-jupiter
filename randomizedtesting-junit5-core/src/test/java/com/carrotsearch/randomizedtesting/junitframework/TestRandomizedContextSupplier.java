package com.carrotsearch.randomizedtesting.junitframework;

import static org.assertj.core.api.Assertions.assertThat;

import com.carrotsearch.randomizedtesting.junitframework.infra.NestedTest;
import com.carrotsearch.randomizedtesting.junitframework.infra.NestedTestResults;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Verifies that {@link RandomizedContextSupplier} properly creates and injects a {@link
 * RandomizedContext} into test methods.
 */
public class TestRandomizedContextSupplier {
  @ExtendWith(RandomizedContextSupplier.class)
  static class ContextParameterInjectionTest extends NestedTest {
    private final RandomizedContext ctx;
    private RandomizedContext ctxBeforeEach;

    public ContextParameterInjectionTest(RandomizedContext ctx) {
      this.ctx = ctx;
    }

    @Test
    void viaConstructor() {
      assertThat(this.ctx).isNotNull();
    }

    @BeforeEach
    void beforeEach(RandomizedContext ctx) {
      this.ctxBeforeEach = Objects.requireNonNull(ctx);
    }

    @Test
    void viaMethodParameter(RandomizedContext ctx) {
      assertThat(ctx).isNotNull();
    }

    @Test
    void viaBeforeEach() {
      assertThat(this.ctxBeforeEach).isNotNull();
    }

    @AfterEach
    void afterEach(RandomizedContext ctx) {
      assertThat(ctx).isNotNull();
      this.ctxBeforeEach = null;
    }
  }

  @Test
  public void contextParameterInjection() {
    assertThat(
            NestedTestResults.execute(ContextParameterInjectionTest.class)
                .asStatusStrings()
                .values())
        .containsOnly("viaMethodParameter: OK", "viaConstructor: OK", "viaBeforeEach: OK");
  }
}
