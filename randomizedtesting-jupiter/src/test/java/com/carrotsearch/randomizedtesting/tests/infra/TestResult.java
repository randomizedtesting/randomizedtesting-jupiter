package com.carrotsearch.randomizedtesting.tests.infra;

/** Single test execution result and payload. */
public record TestResult(String displayName, Status status, String message, String output) {
  public enum Status {
    OK,
    ERROR
  }
}
