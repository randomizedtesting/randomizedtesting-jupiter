package com.carrotsearch.randomizedtesting.jupiter;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This annotation should be placed on classes that want access to {@link RandomizedContext}. This
 * extension injects {@link RandomizedContext} parameter type automatically into test methods and
 * lifecycle hooks.
 */
@Target({ElementType.TYPE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RandomizedContextSupplier.class)
public @interface Randomized {}
