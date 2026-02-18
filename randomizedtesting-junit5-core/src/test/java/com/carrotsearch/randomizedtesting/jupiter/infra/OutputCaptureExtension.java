package com.carrotsearch.randomizedtesting.jupiter.infra;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

/**
 * Extension that resolves {@link PrintWriter} parameters for test methods and captures everything
 * written to them. Captured output is stored in a static map keyed by {@link
 * ExtensionContext#getUniqueId()} so that it can be retrieved after an {@link
 * org.junit.platform.testkit.engine.EngineTestKit} execution.
 */
final class OutputCaptureExtension implements ParameterResolver, AfterEachCallback {
  private static final ConcurrentHashMap<String, String> captured = new ConcurrentHashMap<>();

  private static final ExtensionContext.Namespace NAMESPACE =
      ExtensionContext.Namespace.create(OutputCaptureExtension.class);

  private static final String WRITER_KEY = "stringWriter";

  /** Returns and clears all captured output. */
  static Map<String, String> drain() {
    var result = new ConcurrentHashMap<>(captured);
    captured.clear();
    return result;
  }

  @Override
  public boolean supportsParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    return parameterContext.getParameter().getType().equals(PrintWriter.class);
  }

  @Override
  public Object resolveParameter(
      ParameterContext parameterContext, ExtensionContext extensionContext) {
    var sw = new StringWriter();
    extensionContext.getStore(NAMESPACE).put(WRITER_KEY, sw);
    return new PrintWriter(sw, true);
  }

  @Override
  public void afterEach(ExtensionContext context) {
    var sw = context.getStore(NAMESPACE).get(WRITER_KEY, StringWriter.class);
    if (sw != null) {
      captured.put(context.getUniqueId(), sw.toString());
    }
  }
}
