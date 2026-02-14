package com.carrotsearch.randomizedtesting.junitframework;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;

public class RandomizedContextProviderExtension
    implements TestInstancePreDestroyCallback,
        TestInstancePreConstructCallback,
        InvocationInterceptor,
        ParameterResolver,
        BeforeAllCallback,
        BeforeEachCallback,
        AfterEachCallback,
        AfterAllCallback {
  private static final String KEY_EXECUTION_CONTEXT = ExtensionExecutionContext.class.getName();

  @Override
  public void preConstructTestInstance(
      TestInstanceFactoryContext factoryContext, ExtensionContext extensionContext)
      throws Exception {
    getExecutionContext(extensionContext).push(extensionContext);
  }

  @Override
  public void preDestroyTestInstance(ExtensionContext extensionContext) throws Exception {
    getExecutionContext(extensionContext).pop(extensionContext);
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    System.out.println(extensionContext.getUniqueId());
    var classStore = getClassExtensionStore(extensionContext);
    assert classStore.get(KEY_EXECUTION_CONTEXT) == null;

    var executionContext = new ExtensionExecutionContext(InitialSeed.compute());
    classStore.put(KEY_EXECUTION_CONTEXT, executionContext);

    executionContext.push(extensionContext);
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) {
    var classStore = getClassExtensionStore(extensionContext);
    assert classStore.get(KEY_EXECUTION_CONTEXT) != null;
    var executionContext =
        classStore.remove(KEY_EXECUTION_CONTEXT, ExtensionExecutionContext.class);
    executionContext.pop(extensionContext);
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    getExecutionContext(extensionContext).push(extensionContext);
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    getExecutionContext(extensionContext).pop(extensionContext);
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(RandomizedContext.class)
        && getExecutionContext(extensionContext) != null;
  }

  @Override
  public RandomizedContext resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return getRandomizedContext(extensionContext);
  }

  private RandomizedContext getRandomizedContext(ExtensionContext extensionContext) {
    return getExecutionContext(extensionContext).getContext(extensionContext);
  }

  private ExtensionContext.Store getClassExtensionStore(ExtensionContext extensionContext) {
    return extensionContext.getStore(
        ExtensionContext.StoreScope.EXTENSION_CONTEXT,
        ExtensionContext.Namespace.create(RandomizedContextProviderExtension.class));
  }

  private ExtensionExecutionContext getExecutionContext(ExtensionContext extensionContext) {
    var classStore = getClassExtensionStore(extensionContext);
    return classStore.get(KEY_EXECUTION_CONTEXT, ExtensionExecutionContext.class);
  }
}
