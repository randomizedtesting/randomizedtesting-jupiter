import com.carrotsearch.randomizedtesting.jupiter.internals.RandomizedContextExtension;

module com.carrotsearch.randomizedtesting {
  requires org.junit.jupiter.api;
  requires org.junit.jupiter.params;
  requires java.logging;

  exports com.carrotsearch.randomizedtesting.jupiter;
  exports com.carrotsearch.randomizedtesting.jupiter.generators;

  opens com.carrotsearch.randomizedtesting.jupiter.internals;

  provides org.junit.jupiter.api.extension.Extension with
      RandomizedContextExtension;
}
