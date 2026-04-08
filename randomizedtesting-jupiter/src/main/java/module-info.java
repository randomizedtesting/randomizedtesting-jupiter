import com.carrotsearch.randomizedtesting.jupiter.internals.RandomizedContextExtension;

module com.carrotsearch.randomizedtesting {
  requires org.junit.jupiter.api;
  requires org.junit.jupiter.params;
  requires java.logging;
  requires java.compiler;

  exports com.carrotsearch.randomizedtesting.jupiter;
  exports com.carrotsearch.randomizedtesting.jupiter.generators;

  opens com.carrotsearch.randomizedtesting.jupiter.internals;
  opens com.carrotsearch.randomizedtesting.jupiter to
      org.junit.platform.commons;

  provides org.junit.jupiter.api.extension.Extension with
      RandomizedContextExtension;
}
