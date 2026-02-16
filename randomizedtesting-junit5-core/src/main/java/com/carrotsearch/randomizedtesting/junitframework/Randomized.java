package com.carrotsearch.randomizedtesting.junitframework;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.jupiter.api.ClassTemplate;
import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({ElementType.METHOD, ElementType.TYPE})
@Documented
@ClassTemplate
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(RandomizedContextSupplier.class)
public @interface Randomized {}
