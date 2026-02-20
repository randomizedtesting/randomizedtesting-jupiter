package com.carrotsearch.randomizedtesting.jupiter.infra;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

/**
 * Nested test classes that are used as source classes (with EngineTestKit). The tag is used to
 * ignore those tests for full test runs (from within IDEs, for example).
 */
@Tag("nested-integration-test")
@ExtendWith({OutputCaptureExtension.class})
@IgnoreInStandaloneRuns.EnabledIfConfigParaPresent(
    IgnoreInStandaloneRuns.EnabledIfConfigParaPresent.PARAM_NAME)
public abstract class IgnoreInStandaloneRuns {
  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @ExtendWith(MyConfigParamCondition.class)
  @Inherited
  @interface EnabledIfConfigParaPresent {
    String PARAM_NAME = "nested-integration-test";

    String value();
  }

  static class MyConfigParamCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
      var annotation =
          AnnotationSupport.findAnnotation(context.getElement(), EnabledIfConfigParaPresent.class);
      if (annotation.isEmpty()) {
        return ConditionEvaluationResult.enabled("No annotation");
      }

      String paramName = annotation.get().value();

      return context
          .getConfigurationParameter(paramName)
          .map(
              value ->
                  ConditionEvaluationResult.enabled(
                      "Config param '%s' is present.".formatted(paramName)))
          .orElse(ConditionEvaluationResult.disabled("Config param '%s' not present.", paramName));
    }
  }
}
