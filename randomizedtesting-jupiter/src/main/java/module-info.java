module com.carrotsearch.randomizedtesting {
  requires org.junit.jupiter.api;
  requires org.junit.jupiter.params;

  exports com.carrotsearch.randomizedtesting.jupiter;

  provides org.junit.jupiter.api.extension.Extension
      with com.carrotsearch.randomizedtesting.jupiter.RandomizedContextSupplier;
}
