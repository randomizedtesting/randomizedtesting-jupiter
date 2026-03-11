module com.carrotsearch.randomizedtesting.tests {
  requires org.junit.jupiter.api;
  requires org.junit.jupiter.params;
  requires org.junit.platform.testkit;
  requires org.junit.platform.engine;
  requires java.logging;
  requires com.carrotsearch.randomizedtesting;
  requires net.bytebuddy;

  exports com.carrotsearch.randomizedtesting.tests;

  opens com.carrotsearch.randomizedtesting.tests;
  opens com.carrotsearch.randomizedtesting.tests.infra;
  opens com.carrotsearch.randomizedtesting.tests.experiments;
}
