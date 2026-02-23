package com.carrotsearch.randomizedtesting.jupiter;

import java.util.ServiceLoader;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * A {@link TestEngine} that delegates to JUnit Jupiter and multiplies test execution by running
 * tests in multiple top-level iteration containers, each receiving independently derived random
 * seeds.
 *
 * <p>The number of iterations is controlled by the {@value #ITERATIONS_PROPERTY} configuration
 * parameter (default: {@code 1}). Each iteration's tests have unique context identifiers, causing
 * {@link RandomizedContextSupplier} to derive different seeds per iteration even when a fixed root
 * seed is set via {@link RandomizedContextSupplier.SysProps#TESTS_SEED}.
 */
public class RandomizedTestEngine implements TestEngine {
  /** The unique engine ID ({@value}). */
  public static final String ENGINE_ID = "randomizedtesting-jupiter";

  /** Configuration parameter controlling the number of test iterations. Default: {@code 1}. */
  public static final String ITERATIONS_PROPERTY = "tests.iterations";

  private static final String JUPITER_ENGINE_ID = "junit-jupiter";

  private final TestEngine jupiterEngine = loadJupiterEngine();

  @Override
  public String getId() {
    return ENGINE_ID;
  }

  @Override
  public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
    int iterations =
        request
            .getConfigurationParameters()
            .get(ITERATIONS_PROPERTY)
            .map(Integer::parseInt)
            .orElse(1);

    var engineDescriptor = new EngineDescriptor(uniqueId, "Randomized Testing");
    for (int i = 1; i <= iterations; i++) {
      var iterationUniqueId = uniqueId.append("iteration", String.valueOf(i));
      var jupiterRootId = iterationUniqueId.append("engine", JUPITER_ENGINE_ID);
      var jupiterDescriptor = jupiterEngine.discover(request, jupiterRootId);
      var iterationDescriptor = new IterationDescriptor(iterationUniqueId, i);
      iterationDescriptor.addChild(jupiterDescriptor);
      engineDescriptor.addChild(iterationDescriptor);
    }
    return engineDescriptor;
  }

  @Override
  public void execute(ExecutionRequest request) {
    var engineDescriptor = request.getRootTestDescriptor();
    var listener = request.getEngineExecutionListener();
    listener.executionStarted(engineDescriptor);
    for (var child : engineDescriptor.getChildren()) {
      executeIteration((IterationDescriptor) child, request);
    }
    listener.executionFinished(engineDescriptor, TestExecutionResult.successful());
  }

  private void executeIteration(IterationDescriptor iterationDescriptor, ExecutionRequest request) {
    var listener = request.getEngineExecutionListener();
    listener.executionStarted(iterationDescriptor);
    for (var jupiterDescriptor : iterationDescriptor.getChildren()) {
      jupiterEngine.execute(
          ExecutionRequest.create(
              jupiterDescriptor,
              listener,
              request.getConfigurationParameters(),
              request.getOutputDirectoryCreator(),
              request.getStore(),
              request.getCancellationToken()));
    }
    listener.executionFinished(iterationDescriptor, TestExecutionResult.successful());
  }

  private static TestEngine loadJupiterEngine() {
    return ServiceLoader.load(TestEngine.class).stream()
        .filter(p -> p.type().getName().equals("org.junit.jupiter.engine.JupiterTestEngine"))
        .map(ServiceLoader.Provider::get)
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "JUnit Jupiter engine not found; add junit-jupiter-engine to the classpath"));
  }
}
