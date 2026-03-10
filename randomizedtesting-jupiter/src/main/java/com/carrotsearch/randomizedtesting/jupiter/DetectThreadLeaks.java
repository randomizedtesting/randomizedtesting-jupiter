package com.carrotsearch.randomizedtesting.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Detects threads started within the annotated test class that are still alive after the configured
 * scope ends.
 *
 * <p>Only functional in sequential (same-thread) execution mode. Emits a warning and skips
 * detection if tests run concurrently.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ExtendWith(DetectThreadLeaksExtension.class)
@Inherited
public @interface DetectThreadLeaks {
  /** Scope at which thread leak detection is performed. */
  Scope scope() default Scope.SUITE;

  enum Scope {
    /** Check for leaked threads once after all tests in the class complete. */
    SUITE,
    /** Check for leaked threads after each individual test method. */
    TEST
  }
}
