package com.carrotsearch.randomizedtesting.jupiter;

import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class RandomizedContextSupplier implements ParameterResolver, BeforeAllCallback {
  private static final ExtensionContext.Namespace EXTENSION_NAMESPACE =
      ExtensionContext.Namespace.create(RandomizedContextSupplier.class);

  private static final String KEY_CONTEXT = "randomizedContext";

  /** System properties controlling the extension. */
  public enum SysProps {
    TESTS_SEED("tests.seed");

    public final String propertyKey;

    SysProps(String key) {
      this.propertyKey = key;
    }
  }

  //
  // before-all (class-level context setup).
  //

  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    // Bootstrap the root store's context. Don't know if this can be done
    // in a more elegant way.
    extensionContext
        .getRoot()
        .getStore(EXTENSION_NAMESPACE)
        .computeIfAbsent(
            KEY_CONTEXT,
            unused -> {
              var firstAndRest =
                  SeedChain.parse(
                          extensionContext
                              .getConfigurationParameter(SysProps.TESTS_SEED.propertyKey)
                              .orElse("*"))
                      .pop();
              var initialSeed = firstAndRest.first();
              if (initialSeed.isUnspecified()) {
                initialSeed = new Seed(new Random().nextLong());
              }
              return new RandomizedContext(
                  extensionContext.getRoot().getUniqueId(),
                  null,
                  Thread.currentThread(),
                  Random::new,
                  initialSeed,
                  firstAndRest.rest());
            });
  }

  //
  // ParameterResolver: inject RandomizedContext into test methods.
  //

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return parameterContext.getParameter().getType().equals(RandomizedContext.class);
  }

  @Override
  public RandomizedContext resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    return getRandomizedContextFor(extensionContext);
  }

  //
  // internal handling of randomized contexts within junit contexts.
  //

  private RandomizedContext getRandomizedContextFor(ExtensionContext extensionContext) {
    var store =
        extensionContext.getStore(
            ExtensionContext.StoreScope.EXTENSION_CONTEXT, EXTENSION_NAMESPACE);

    var thisContext = Objects.requireNonNull(store.get(KEY_CONTEXT, RandomizedContext.class));
    if (extensionContext.getUniqueId().equals(thisContext.contextId)) {
      return thisContext;
    }

    // No context for this context yet.
    var parentContext = getRandomizedContextFor(extensionContext.getParent().orElseThrow());
    thisContext = parentContext.deriveNew(Thread.currentThread(), extensionContext);
    store.put(KEY_CONTEXT, thisContext);
    return thisContext;
  }
}
