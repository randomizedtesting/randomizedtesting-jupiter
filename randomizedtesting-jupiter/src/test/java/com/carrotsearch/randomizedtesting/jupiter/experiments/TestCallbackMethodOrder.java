package com.carrotsearch.randomizedtesting.jupiter.experiments;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeClassTemplateInvocationCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.DynamicTestInvocationContext;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestInstanceFactoryContext;
import org.junit.jupiter.api.extension.TestInstancePostProcessor;
import org.junit.jupiter.api.extension.TestInstancePreConstructCallback;
import org.junit.jupiter.api.extension.TestInstancePreDestroyCallback;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.ValueSource;

@Disabled
@ExtendWith(TestCallbackMethodOrder.DebugExt.class)
@ParameterizedClass
@ValueSource(strings = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"})
public class TestCallbackMethodOrder {
  static {
    System.out.println("Static constructor.");
  }

  public TestCallbackMethodOrder(String param) {
    System.out.println("constructor: " + param);
  }

  @BeforeAll
  public static void beforeAll() {
    System.out.println("Before all.");
  }

  @AfterAll
  public static void afterAll() {
    System.out.println("After all.");
  }

  @BeforeEach
  public void before() {}

  @AfterEach
  public void after() {}

  @Test
  public void b() {
    System.out.println("Test b.");
  }

  @Test
  public void a() {
    System.out.println("Test a.");
  }

  public static class DebugExt
      implements BeforeAllCallback,
          AfterAllCallback,
          BeforeEachCallback,
          AfterEachCallback,
          BeforeTestExecutionCallback,
          BeforeClassTemplateInvocationCallback,
          AfterTestExecutionCallback,
          TestInstancePostProcessor,
          TestInstancePreConstructCallback,
          TestInstancePreDestroyCallback,
          InvocationInterceptor {

    private static void log(String callback, ExtensionContext context) {
      System.out.println(callback + " | " + context.getUniqueId());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
      log("beforeAll", context);
    }

    @Override
    public void afterAll(ExtensionContext context) {
      log("afterAll", context);
    }

    @Override
    public void beforeEach(ExtensionContext context) {
      log("beforeEach", context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
      log("afterEach", context);
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
      log("beforeTestExecution", context);
    }

    @Override
    public void afterTestExecution(ExtensionContext context) {
      log("afterTestExecution", context);
    }

    @Override
    public void preConstructTestInstance(
        TestInstanceFactoryContext factoryContext, ExtensionContext context) {
      log("preConstructTestInstance", context);
    }

    @Override
    public void postProcessTestInstance(Object testInstance, ExtensionContext context) {
      log("postProcessTestInstance", context);
    }

    @Override
    public void preDestroyTestInstance(ExtensionContext context) {
      log("preDestroyTestInstance", context);
    }

    @Override
    public <T> T interceptTestClassConstructor(
        Invocation<T> invocation,
        ReflectiveInvocationContext<Constructor<T>> invocationContext,
        ExtensionContext extensionContext)
        throws Throwable {
      log("interceptTestClassConstructor", extensionContext);
      return invocation.proceed();
    }

    @Override
    public void interceptBeforeAllMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext)
        throws Throwable {
      log("interceptBeforeAllMethod", extensionContext);
      invocation.proceed();
    }

    @Override
    public void interceptBeforeEachMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext)
        throws Throwable {
      log("interceptBeforeEachMethod", extensionContext);
      invocation.proceed();
    }

    @Override
    public void interceptTestMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext)
        throws Throwable {
      log("interceptTestMethod", extensionContext);
      invocation.proceed();
    }

    @Override
    public <T> T interceptTestFactoryMethod(
        Invocation<T> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext)
        throws Throwable {
      log("interceptTestFactoryMethod", extensionContext);
      return invocation.proceed();
    }

    @Override
    public void interceptTestTemplateMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext)
        throws Throwable {
      log("interceptTestTemplateMethod", extensionContext);
      invocation.proceed();
    }

    @Override
    public void interceptDynamicTest(
        Invocation<Void> invocation,
        DynamicTestInvocationContext invocationContext,
        ExtensionContext extensionContext)
        throws Throwable {
      log("interceptDynamicTest", extensionContext);
      invocation.proceed();
    }

    @Override
    public void interceptAfterEachMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext)
        throws Throwable {
      log("interceptAfterEachMethod", extensionContext);
      invocation.proceed();
    }

    @Override
    public void interceptAfterAllMethod(
        Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext,
        ExtensionContext extensionContext)
        throws Throwable {
      log("interceptAfterAllMethod", extensionContext);
      invocation.proceed();
    }

    @Override
    public void beforeClassTemplateInvocation(ExtensionContext extensionContext) throws Exception {
      log("beforeClassTemplateInvocation", extensionContext);
    }
  }
}
