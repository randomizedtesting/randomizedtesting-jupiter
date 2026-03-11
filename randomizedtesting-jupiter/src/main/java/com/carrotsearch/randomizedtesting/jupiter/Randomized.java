package com.carrotsearch.randomizedtesting.jupiter;

import com.carrotsearch.randomizedtesting.jupiter.internals.RandomizedContextExtension;
import com.carrotsearch.randomizedtesting.jupiter.internals.RandomizedContextImpl;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * This annotation should be placed on classes that want access to {@link RandomizedContextImpl}.
 * This extension injects {@link RandomizedContextImpl} parameter type automatically into test
 * methods and lifecycle hooks.
 */
@Target({ElementType.TYPE})
@Documented
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RandomizedContextExtension.class)
public @interface Randomized {}
