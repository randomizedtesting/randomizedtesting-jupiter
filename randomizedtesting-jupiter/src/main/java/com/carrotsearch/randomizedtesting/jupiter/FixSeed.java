package com.carrotsearch.randomizedtesting.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This annotation should be placed on classes or methods that are {@link Randomized} and would like
 * to use a constant seed (for reproducing a problem or other reasons).
 *
 * <p>Note that seed fixing is always possible by setting {@link
 * com.carrotsearch.randomizedtesting.jupiter.RandomizedContextSupplier.SysProps#TESTS_SEED} system
 * property, this is just convenience.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RandomizedContextSupplier.class)
public @interface FixSeed {
  String value();
}
