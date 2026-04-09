package com.carrotsearch.randomizedtesting.jupiter;

import java.util.ServiceLoader;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * An experimental {@link TestEngine} that delegates to JUnit Jupiter and multiplies test execution
 * by re-running tests in multiple top-level jupiter engines.
 *
 * <p><strong>This is an experimental class and an experimental implementation.</strong>
 *
 * <p>The number of iterations is controlled by the {@link SysProps#TESTS_ITERS} configuration
 * parameter. The default value (0) means no test are executed.
 */
public final class RepeatExecutionTestEngine implements TestEngine {
  /** The unique engine ID ({@value}). */
  public static final String ENGINE_ID = "randomizedtesting-jupiter";

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
            .get(SysProps.TESTS_ITERS.propertyKey)
            .map(Integer::parseInt)
            .orElse(0);

    UniqueId.Segment jupiterRootSegment;
    {
      var jupiterRootSegments = UniqueId.forEngine(JUPITER_ENGINE_ID).getSegments();
      assert jupiterRootSegments.size() == 1;
      jupiterRootSegment = jupiterRootSegments.getFirst();
    }

    var engineDescriptor = new EngineDescriptor(uniqueId, "RandomizedTesting");
    for (int i = 1; i <= iterations; i++) {
      var iterationUniqueId =
          uniqueId.append(ReiterationDescriptor.SEGMENT_TYPE, String.valueOf(i));
      var jupiterDescriptor =
          jupiterEngine.discover(request, iterationUniqueId.append(jupiterRootSegment));

      var iterationDescriptor = new ReiterationDescriptor(iterationUniqueId, i);
      iterationDescriptor.addChild(jupiterDescriptor);
      engineDescriptor.addChild(iterationDescriptor);
    }

    return engineDescriptor;
  }

  public static class ReiterationDescriptor extends AbstractTestDescriptor {
    public static final String SEGMENT_TYPE = "reiteration";

    public ReiterationDescriptor(UniqueId uniqueId, long iteration) {
      super(uniqueId, "Iteration " + iteration);
    }

    @Override
    public Type getType() {
      return Type.CONTAINER;
    }
  }

  @Override
  public void execute(ExecutionRequest request) {
    var engineDescriptor = request.getRootTestDescriptor();
    var listener = request.getEngineExecutionListener();
    listener.executionStarted(engineDescriptor);
    for (var child : engineDescriptor.getChildren()) {
      executeIteration((ReiterationDescriptor) child, request);
    }
    listener.executionFinished(engineDescriptor, TestExecutionResult.successful());
  }

  private void executeIteration(
      ReiterationDescriptor iterationDescriptor, ExecutionRequest request) {
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
