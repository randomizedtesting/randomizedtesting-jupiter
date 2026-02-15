package com.carrotsearch.randomizedtesting.junitframework;

import static org.assertj.core.api.Assertions.assertThat;

import com.carrotsearch.randomizedtesting.junitframework.infra.NestedTest;
import com.carrotsearch.randomizedtesting.junitframework.infra.NestedTestResults;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Verifies that {@link RandomizedContextSupplier} properly creates and injects a {@link
 * RandomizedContext} into test methods.
 */
public class TestExperiments {
  @ParameterizedTest
  @ValueSource(strings = { "a", "b", "c" })
  public void contextParameterInjection(String x) {
    System.out.println(x);
  }
}
