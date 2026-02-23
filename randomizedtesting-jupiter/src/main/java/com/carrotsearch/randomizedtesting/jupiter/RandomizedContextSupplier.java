package com.carrotsearch.randomizedtesting.jupiter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.LifecycleMethodExecutionExceptionHandler;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;

public class RandomizedContextSupplier
    implements ParameterResolver,
        BeforeAllCallback,
        TestExecutionExceptionHandler,
        LifecycleMethodExecutionExceptionHandler {
  private static final ExtensionContext.Namespace EXTENSION_NAMESPACE =
      ExtensionContext.Namespace.create(RandomizedContextSupplier.class);

  private static final String CTX_KEY_RANDOMIZED_CONTEXT = "randomizedContext";

  /** System properties controlling the extension. */
  public enum SysProps {
    /** Initial root seed value. If empty, a random value is picked for the root seed. */
    TESTS_SEED("tests.seed"),

    /**
     * String name of the factory used to create {@link Random} instances (see {@link
     * RandomFactoryType} for named implementations).
     *
     * @see RandomFactoryType
     */
    TESTS_RANDOM_FACTORY("tests.random.factory"),

    /**
     * A boolean property that enables stricter sanity assertions (including forbidding
     * thread-shared access to the returned {@link Random} instances, which makes tests more
     * predictable).
     */
    TESTS_RANDOM_ASSERTING("tests.random.asserting");

    public final String propertyKey;

    SysProps(String key) {
      this.propertyKey = key;
    }
  }

  public enum RandomFactoryType implements Supplier<RandomFactory> {
    JDK,
    XOROSHIRO_128_PLUS;

    public static RandomFactoryType parse(String v) {
      try {
        return RandomFactoryType.valueOf(v.toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Can't parse "
                + SysProps.TESTS_RANDOM_FACTORY.propertyKey
                + " property: "
                + v
                + " [valid values: "
                + Stream.of(RandomFactoryType.values())
                    .map(vv -> vv.name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(", "))
                + "]");
      }
    }

    @Override
    public RandomFactory get() {
      return switch (this) {
        case JDK -> Random::new;
        case XOROSHIRO_128_PLUS -> Xoroshiro128PlusRandom::new;
      };
    }
  }

  //
  // before-all (class-level context setup).
  //

  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    // Set up Random factory.
    RandomFactory randomFactory = initializeRandomFactory(extensionContext);

    // Bootstrap the root store's context. Don't know if this can be done
    // in a more elegant way.
    extensionContext
        .getRoot()
        .getStore(EXTENSION_NAMESPACE)
        .computeIfAbsent(
            CTX_KEY_RANDOMIZED_CONTEXT,
            unused -> {
              var firstAndRest =
                  parseRootSeed(
                          extensionContext.getConfigurationParameter(
                              SysProps.TESTS_SEED.propertyKey))
                      .pop();
              assert !firstAndRest.first().isUnspecified();

              return new RandomizedContext(
                  extensionContext.getRoot().getUniqueId(),
                  null,
                  randomFactory,
                  firstAndRest.first(),
                  firstAndRest.rest());
            });
  }

  private static RandomFactory initializeRandomFactory(ExtensionContext extensionContext) {
    RandomFactory randomFactory =
        extensionContext
            .getConfigurationParameter(SysProps.TESTS_RANDOM_FACTORY.propertyKey)
            .map(RandomFactoryType::parse)
            .orElse(RandomFactoryType.XOROSHIRO_128_PLUS)
            .get();

    if (extensionContext
        .getConfigurationParameter(SysProps.TESTS_RANDOM_ASSERTING.propertyKey)
        .map(Boolean::parseBoolean)
        .orElse(RandomizedContextSupplier.class.desiredAssertionStatus())) {
      var delegateFactory = randomFactory;
      randomFactory =
          seed -> new AssertingRandom(Thread.currentThread(), delegateFactory.apply(seed));
    }

    return randomFactory;
  }

  /**
   * @return Returns the constant root seed, initialized from an optional configuration parameter.
   */
  private static SeedChain parseRootSeed(Optional<String> rootSeedValue) {
    var seedChain = SeedChain.parse(rootSeedValue.orElse("*"));
    var firstAndRest = seedChain.pop();
    if (firstAndRest.first().isUnspecified()) {
      var recreateChain = new ArrayList<Seed>();
      recreateChain.add(new Seed(new Random().nextLong()));
      recreateChain.addAll(firstAndRest.rest().seeds());
      seedChain = new SeedChain(recreateChain);
    }
    return seedChain;
  }

  //
  // ParameterResolver: inject RandomizedContext and Random instances into test methods.
  //

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    Class<?> parameterType = parameterContext.getParameter().getType();
    return parameterType.equals(RandomizedContext.class) || parameterType.equals(Random.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext)
      throws ParameterResolutionException {
    var ctx = getRandomizedContextFor(extensionContext);
    Class<?> parameterType = parameterContext.getParameter().getType();
    if (parameterType.equals(RandomizedContext.class)) {
      return ctx;
    } else if (parameterType.equals(Random.class)) {
      return ctx.getRandom();
    } else {
      throw new RuntimeException(
          "Unexpected unsupported parameter type in resolveParameter: " + parameterType);
    }
  }

  //
  // internal handling of randomized contexts within junit contexts.
  //

  private RandomizedContext getRandomizedContextFor(ExtensionContext extensionContext) {
    var store =
        extensionContext.getStore(
            ExtensionContext.StoreScope.EXTENSION_CONTEXT, EXTENSION_NAMESPACE);

    var thisContext =
        Objects.requireNonNull(store.get(CTX_KEY_RANDOMIZED_CONTEXT, RandomizedContext.class));
    if (extensionContext.getUniqueId().equals(thisContext.contextId)) {
      return thisContext;
    }

    // No context for this context yet.
    var parentContext = getRandomizedContextFor(extensionContext.getParent().orElseThrow());
    thisContext = parentContext.deriveNew(extensionContext);
    store.put(CTX_KEY_RANDOMIZED_CONTEXT, thisContext);
    return thisContext;
  }

  //
  // exception handling and seed stack frame injection
  //

  static final String AUGMENTED_SEED_CLASS = "__randomizedtesting.SeedChain";

  @Override
  public void handleTestExecutionException(ExtensionContext context, Throwable throwable)
      throws Throwable {
    throw addSeedChainStackFrame(throwable, getRandomizedContextFor(context).getSeedChain());
  }

  @Override
  public void handleBeforeAllMethodExecutionException(ExtensionContext context, Throwable throwable)
      throws Throwable {
    throw addSeedChainStackFrame(throwable, getRandomizedContextFor(context).getSeedChain());
  }

  @Override
  public void handleBeforeEachMethodExecutionException(
      ExtensionContext context, Throwable throwable) throws Throwable {
    throw addSeedChainStackFrame(throwable, getRandomizedContextFor(context).getSeedChain());
  }

  @Override
  public void handleAfterEachMethodExecutionException(ExtensionContext context, Throwable throwable)
      throws Throwable {
    throw addSeedChainStackFrame(throwable, getRandomizedContextFor(context).getSeedChain());
  }

  @Override
  public void handleAfterAllMethodExecutionException(ExtensionContext context, Throwable throwable)
      throws Throwable {
    throw addSeedChainStackFrame(throwable, getRandomizedContextFor(context).getSeedChain());
  }

  private Throwable addSeedChainStackFrame(Throwable throwable, SeedChain seedChain) {
    List<StackTraceElement> stack = new ArrayList<>(Arrays.asList(throwable.getStackTrace()));
    stack.addFirst(new StackTraceElement(AUGMENTED_SEED_CLASS, "seed", seedChain.toString(), -1));
    throwable.setStackTrace(stack.toArray(StackTraceElement[]::new));
    return throwable;
  }
}
