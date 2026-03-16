package com.carrotsearch.randomizedtesting.jupiter.internals;

import com.carrotsearch.randomizedtesting.jupiter.Constants;
import com.carrotsearch.randomizedtesting.jupiter.RandomInstanceFactory;
import com.carrotsearch.randomizedtesting.jupiter.RandomizedContext;
import com.carrotsearch.randomizedtesting.jupiter.Seed;
import com.carrotsearch.randomizedtesting.jupiter.SeedChain;
import com.carrotsearch.randomizedtesting.jupiter.SysProps;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.LongFunction;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

public class RandomizedContextExtension
    implements ParameterResolver, BeforeAllCallback, InvocationInterceptor {
  private static final ExtensionContext.Namespace EXTENSION_NAMESPACE =
      ExtensionContext.Namespace.create(RandomizedContextExtension.class);

  private static final String CTX_KEY_RANDOMIZED_CONTEXT = "randomizedContext";

  //
  // before-all (class-level context setup).
  //

  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    // Set up Random factory.
    var randomFactory = initializeRandomFactory(extensionContext);

    // Bootstrap the root store's context. Don't know if this can be done
    // in a more elegant way.
    extensionContext
        .getRoot()
        .getStore(EXTENSION_NAMESPACE)
        .computeIfAbsent(
            CTX_KEY_RANDOMIZED_CONTEXT,
            unused -> {
              var firstAndRest =
                  FirstAndRest.from(
                      parseRootSeed(
                          extensionContext.getConfigurationParameter(
                              SysProps.TESTS_SEED.propertyKey)));
              assert !firstAndRest.first().isUnspecified();

              return new RandomizedContextImpl(
                  extensionContext.getRoot().getUniqueId(),
                  null,
                  randomFactory,
                  firstAndRest.first(),
                  firstAndRest.rest());
            });
  }

  private static LongFunction<Random> initializeRandomFactory(ExtensionContext extensionContext) {
    var randomFactory =
        extensionContext
            .getConfigurationParameter(SysProps.TESTS_RANDOM_FACTORY.propertyKey)
            .map(RandomInstanceFactory::parse)
            .orElse(RandomInstanceFactory.XOROSHIRO_128_PLUS)
            .get();

    if (extensionContext
        .getConfigurationParameter(SysProps.TESTS_RANDOM_ASSERTING.propertyKey)
        .map(Boolean::parseBoolean)
        .orElse(RandomizedContextExtension.class.desiredAssertionStatus())) {
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
    var firstAndRest = FirstAndRest.from(seedChain);
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

  private static RandomizedContextImpl getRandomizedContextFor(ExtensionContext extensionContext) {
    var store =
        extensionContext.getStore(
            ExtensionContext.StoreScope.EXTENSION_CONTEXT, EXTENSION_NAMESPACE);

    var thisContext =
        Objects.requireNonNull(store.get(CTX_KEY_RANDOMIZED_CONTEXT, RandomizedContextImpl.class));
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

  private static <T> T wrapInvoke(Invocation<T> invocation, ExtensionContext context)
      throws Throwable {
    try {
      return invocation.proceed();
    } catch (Throwable throwable) {
      throw addSeedChainStackFrame(throwable, getRandomizedContextFor(context).getSeedChain());
    }
  }

  @Override
  public <T> T interceptTestClassConstructor(
      Invocation<T> invocation,
      ReflectiveInvocationContext<Constructor<T>> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    return wrapInvoke(invocation, extensionContext);
  }

  @Override
  public void interceptBeforeAllMethod(
      Invocation<@Nullable Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    wrapInvoke(invocation, extensionContext);
  }

  @Override
  public void interceptBeforeEachMethod(
      Invocation<@Nullable Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    wrapInvoke(invocation, extensionContext);
  }

  @Override
  public void interceptTestMethod(
      Invocation<@Nullable Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    wrapInvoke(invocation, extensionContext);
  }

  @Override
  public <T> T interceptTestFactoryMethod(
      Invocation<T> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    return wrapInvoke(invocation, extensionContext);
  }

  @Override
  public void interceptTestTemplateMethod(
      Invocation<@Nullable Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    wrapInvoke(invocation, extensionContext);
  }

  @Override
  public void interceptAfterEachMethod(
      Invocation<@Nullable Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    wrapInvoke(invocation, extensionContext);
  }

  @Override
  public void interceptAfterAllMethod(
      Invocation<@Nullable Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    wrapInvoke(invocation, extensionContext);
  }

  @Override
  public void interceptDynamicTest(
      Invocation<@Nullable Void> invocation,
      DynamicTestInvocationContext invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    wrapInvoke(invocation, extensionContext);
  }

  private static Throwable addSeedChainStackFrame(Throwable throwable, SeedChain seedChain) {
    List<StackTraceElement> stack = new ArrayList<>(Arrays.asList(throwable.getStackTrace()));
    stack.addFirst(
        new StackTraceElement(Constants.AUGMENTED_SEED_CLASS, "seed", seedChain.toString(), -1));
    throwable.setStackTrace(stack.toArray(StackTraceElement[]::new));
    return throwable;
  }
}
