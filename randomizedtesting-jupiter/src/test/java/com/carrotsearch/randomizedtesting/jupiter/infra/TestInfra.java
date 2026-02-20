package com.carrotsearch.randomizedtesting.jupiter.infra;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.Map;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Runs a nested test class via {@link EngineTestKit} and collects structured {@link TestResult}s.
 */
public final class TestInfra {
  public record ExecutionResult(
      EngineExecutionResults results, Map<String, String> capturedOutput) {}

  public static ExecutionResult collectExecutionResults(EngineTestKit.Builder builder) {
    var events = builder.execute();
    var capturedOutput = OutputCaptureExtension.drain();
    return new ExecutionResult(events, capturedOutput);
  }

  public static EngineTestKit.Builder testKitBuilder(Class<?> testClass) {
    return testKitBuilder().selectors(selectClass(testClass));
  }

  public static EngineTestKit.Builder testKitBuilder() {
    OutputCaptureExtension.drain();
    return EngineTestKit.engine(new JupiterTestEngine())
        .configurationParameter(
            IgnoreInStandaloneRuns.EnabledIfConfigParaPresent.PARAM_NAME, "true");
  }
}
