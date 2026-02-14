package com.carrotsearch.randomizedtesting.junitframework;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class RandomizedContextSupplier implements ParameterResolver {
  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(RandomizedContextSupplier.class);

  //
  // ParameterResolver overrides.
  //

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(RandomizedContext.class)
        && getRandomizedContext(extensionContext) != null;
  }

  @Override
  public RandomizedContext resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return getRandomizedContext(extensionContext);
  }

  //
  // Handling of randomized contexts within junit contexts.
  //

  private RandomizedContext getRandomizedContext(ExtensionContext extensionContext) {
    return new RandomizedContext(Thread.currentThread(), new RandomSeed(0xdeadbeef));
  }
}
