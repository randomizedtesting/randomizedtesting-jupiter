package com.carrotsearch.randomizedtesting.jupiter.infra;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Nested test classes that are used as sources (with EngineTestKit). The tag is used to ignore
 * those tests for test runs.
 */
@Tag("nested-integration-test")
@ExtendWith(OutputCaptureExtension.class)
public abstract class NestedTest {}
