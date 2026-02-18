package com.carrotsearch.randomizedtesting.junitframework;

import java.util.Objects;
import java.util.Random;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;

public class RandomizedContextSupplier
    implements ParameterResolver, BeforeAllCallback, BeforeEachCallback {
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
  // Bootstrap initialization.
  //

  //
  // before-all (class-level context setup).
  //

  @Override
  public void beforeAll(ExtensionContext extensionContext) throws Exception {
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

    pushNestedRandomizedContext(extensionContext);
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) throws Exception {
    pushNestedRandomizedContext(extensionContext);
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

  private void pushNestedRandomizedContext(ExtensionContext extensionContext) {
    var store =
        Objects.requireNonNull(
            extensionContext.getStore(
                ExtensionContext.StoreScope.EXTENSION_CONTEXT, EXTENSION_NAMESPACE));

    store.put(
        KEY_CONTEXT,
        store
            .get(KEY_CONTEXT, RandomizedContext.class)
            .deriveNew(Thread.currentThread(), extensionContext));
  }

  private RandomizedContext getRandomizedContextFor(ExtensionContext extensionContext) {
    var store =
        Objects.requireNonNull(
            extensionContext.getStore(
                ExtensionContext.StoreScope.EXTENSION_CONTEXT, EXTENSION_NAMESPACE));
    return store.get(KEY_CONTEXT, RandomizedContext.class);
  }
}
