package com.carrotsearch.randomizedtesting.jupiter;

import com.carrotsearch.randomizedtesting.jupiter.internals.DetectThreadLeaksExtension;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Predicate;
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
    /** Disable thread leak detection entirely. */
    NONE,
    /** Check for leaked threads once after all tests in the class complete. */
    SUITE,
    /** Check for leaked threads after each individual test method. */
    TEST
  }

  /**
   * Milliseconds to wait for leaked threads to self-terminate before declaring a failure. If all
   * leaked threads terminate within this window, the test passes. Default is 0 (no lingering).
   *
   * <p>Place this annotation on the same class or method as {@link DetectThreadLeaks}. A
   * method-level annotation takes precedence over a class-level one.
   */
  @Target({ElementType.TYPE, ElementType.METHOD})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @Inherited
  @interface LingerTime {
    int millis();
  }

  /**
   * Excludes threads matched by any of the given {@link Predicate} classes from leak detection. A
   * thread is excluded when at least one predicate returns {@code true} for it.
   *
   * <p>Annotations are collected hierarchically from the class and its superclasses, and the
   * filters from all levels are combined.
   *
   * @see SystemThreadFilter
   */
  @Target({ElementType.TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  @Documented
  @interface ExcludeThreads {
    Class<? extends Predicate<Thread>>[] value() default {SystemThreadFilter.class};
  }
}
