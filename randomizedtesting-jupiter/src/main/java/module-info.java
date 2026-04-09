import com.carrotsearch.randomizedtesting.jupiter.RepeatExecutionTestEngine;

module com.carrotsearch.randomizedtesting {
  requires org.junit.jupiter.api;
  requires org.junit.jupiter.params;
  requires java.logging;

  exports com.carrotsearch.randomizedtesting.jupiter;
  exports com.carrotsearch.randomizedtesting.jupiter.generators;

  opens com.carrotsearch.randomizedtesting.jupiter.internals;
  opens com.carrotsearch.randomizedtesting.jupiter to
      org.junit.platform.commons;

  provides org.junit.jupiter.api.extension.Extension with
      com.carrotsearch.randomizedtesting.jupiter.internals.RandomizedContextExtension;

  // These entries install support for tests.iters (RepeatedExecutionTestEngine).
  requires org.junit.platform.engine;

  uses org.junit.platform.engine.TestEngine;

  provides org.junit.platform.engine.TestEngine with
      RepeatExecutionTestEngine;
}
