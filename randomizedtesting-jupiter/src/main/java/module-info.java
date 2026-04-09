import com.carrotsearch.randomizedtesting.jupiter.RepeatExecutionTestEngine;

module com.carrotsearch.randomizedtesting {
  requires org.junit.jupiter.api;
  requires org.junit.jupiter.params;
  requires org.junit.platform.engine;
  requires java.logging;

  exports com.carrotsearch.randomizedtesting.jupiter;
  exports com.carrotsearch.randomizedtesting.jupiter.generators;

  opens com.carrotsearch.randomizedtesting.jupiter.internals;
  opens com.carrotsearch.randomizedtesting.jupiter to
      org.junit.platform.commons;

  uses org.junit.platform.engine.TestEngine;

  provides org.junit.jupiter.api.extension.Extension with
      com.carrotsearch.randomizedtesting.jupiter.internals.RandomizedContextExtension;
  provides org.junit.platform.engine.TestEngine with
      RepeatExecutionTestEngine;
}
