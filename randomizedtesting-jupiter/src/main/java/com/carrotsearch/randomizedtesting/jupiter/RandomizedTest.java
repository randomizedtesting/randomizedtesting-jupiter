package com.carrotsearch.randomizedtesting.jupiter;

import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Randomized
@DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
@Execution(value = ExecutionMode.SAME_THREAD, reason = "Backward compatibility.")
public abstract class RandomizedTest {}
