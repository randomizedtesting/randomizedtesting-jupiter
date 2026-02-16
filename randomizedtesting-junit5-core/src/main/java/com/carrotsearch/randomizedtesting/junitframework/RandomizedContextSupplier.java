package com.carrotsearch.randomizedtesting.junitframework;

import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ClassTemplateInvocationContext;
import org.junit.jupiter.api.extension.ClassTemplateInvocationContextProvider;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;
import org.junit.jupiter.api.extension.TestTemplateInvocationContextProvider;

public class RandomizedContextSupplier
    implements ParameterResolver, ClassTemplateInvocationContextProvider {
  @Override
  public boolean supportsClassTemplate(ExtensionContext context) {
    return isAnnotated(context.getRequiredTestClass(), Randomized.class);
  }

  @Override
  public Stream<? extends ClassTemplateInvocationContext> provideClassTemplateInvocationContexts(
      ExtensionContext context) {
    return Stream.of(
        new ClassTemplateInvocationContext() {
          @Override
          public void prepareInvocation(ExtensionContext context) {
            ClassTemplateInvocationContext.super.prepareInvocation(context);
          }

          @Override
          public String getDisplayName(int invocationIndex) {
            return "[seed: 0xdeadbeef]";
          }
        });
  }

  /*
  @Override
  public void beforeAll(ExtensionContext context) throws Exception {
    System.out.println("beforeAll: " + context.getUniqueId());
    context
        .getRoot()
        .getStore(
            ExtensionContext.StoreScope.EXTENSION_CONTEXT,
            ExtensionContext.Namespace.create("randomizedtesting"))
        .put("foo", "bar");
  }

  @Override
  public void beforeEach(ExtensionContext context) throws Exception {
    System.out.println("beforeEach: " + context.getUniqueId());
    context
        .getStore(
            ExtensionContext.StoreScope.EXTENSION_CONTEXT,
            ExtensionContext.Namespace.create("randomizedtesting"))
        .put("foo", "bar: " + context.getUniqueId());
  }
   */

  //
  // ParameterResolver: inject RandomizedContext into test methods.
  //

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    System.out.println("supportsParameter: " + extensionContext.getUniqueId());
    System.out.println(
        extensionContext
            .getStore(ExtensionContext.Namespace.create("randomizedtesting"))
            .get("foo"));

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
  // internal handling of randomized contexts within junit contexts.
  //

  private RandomizedContext getRandomizedContext(ExtensionContext extensionContext) {
    return new RandomizedContext(Thread.currentThread(), new RandomSeed(0xdeadbeef));
  }
}
