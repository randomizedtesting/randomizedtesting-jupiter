package com.carrotsearch.randomizedtesting.jupiter.infra;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Events;

/**
 * Runs a nested test class via {@link EngineTestKit} and collects structured {@link TestResult}s.
 */
public class NestedTestResults {
  public record ExecutionResult(Events events, Map<String, String> capturedOutput) {
    public Map<String, TestResult> testResults() {
      var map = new LinkedHashMap<String, TestResult>();
      for (var event : events.list()) {
        var descriptor = event.getTestDescriptor();
        var methodName =
            descriptor
                .getSource()
                .filter(MethodSource.class::isInstance)
                .map(s -> ((MethodSource) s).getMethodName())
                .orElseGet(descriptor::getDisplayName);
        var result = event.getRequiredPayload(TestExecutionResult.class);
        var status =
            result.getStatus() == TestExecutionResult.Status.SUCCESSFUL
                ? TestResult.Status.OK
                : TestResult.Status.ERROR;
        var message = result.getThrowable().map(Throwable::getMessage).orElse(null);
        var output = capturedOutput.get(descriptor.getUniqueId().toString());
        map.put(methodName, new TestResult(methodName, status, message, output));
      }

      return map;
    }

    public Map<String, String> testResultsAsStatusStrings() {
      return testResults().entrySet().stream()
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  e ->
                      String.format(
                          Locale.ROOT,
                          "%s: %s",
                          e.getValue().displayName(),
                          e.getValue().status())));
    }
  }

  public static ExecutionResult collectExecutionResults(EngineTestKit.Builder builder) {
    var events = builder.execute().testEvents().finished();
    var capturedOutput = OutputCaptureExtension.drain();
    return new ExecutionResult(events, capturedOutput);
  }

  public static EngineTestKit.Builder testKitBuilder(Class<?> testClass) {
    OutputCaptureExtension.drain();
    return EngineTestKit.engine(new JupiterTestEngine()).selectors(selectClass(testClass));
  }
}
