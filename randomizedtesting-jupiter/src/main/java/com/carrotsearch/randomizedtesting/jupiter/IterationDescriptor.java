package com.carrotsearch.randomizedtesting.jupiter;

import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/** A test descriptor representing one reiteration of the test suite. */
final class IterationDescriptor extends AbstractTestDescriptor {
  private final int iteration;

  IterationDescriptor(UniqueId uniqueId, int iteration) {
    super(uniqueId, "Iteration #" + iteration);
    this.iteration = iteration;
  }

  @Override
  public Type getType() {
    return Type.CONTAINER;
  }

  int getIteration() {
    return iteration;
  }
}
