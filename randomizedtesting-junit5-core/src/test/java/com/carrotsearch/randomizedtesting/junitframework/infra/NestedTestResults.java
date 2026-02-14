package com.carrotsearch.randomizedtesting.junitframework.infra;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.engine.JupiterTestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.testkit.engine.EngineTestKit;

/**
 * Runs a nested test class via {@link EngineTestKit} and collects structured {@link TestResult}s.
 */
public class NestedTestResults {
  public record ExecutionResult(Map<String, TestResult> results) {
    public Map<String, String> asStatusStrings() {
      return results.entrySet().stream()
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

  public static ExecutionResult execute(Class<?> testClass) {
    OutputCaptureExtension.drain();

    var events =
        EngineTestKit.engine(new JupiterTestEngine())
            .selectors(selectClass(testClass))
            .execute()
            .testEvents()
            .finished();

    var capturedOutput = OutputCaptureExtension.drain();

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
    return new ExecutionResult(map);
  }
}
