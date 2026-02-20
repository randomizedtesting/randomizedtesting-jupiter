# Feature: seed recovery

## Functionality

* It must be possible to acquire the value of the root seed using
  `RandomizedContext.getRootSeed()`.

* When tests are run with an unspecified (random) root seed, it must be possible
  to recover its value when a test fails.

* The contextual seed value should be
  inserted as a synthetic exception stack frame in any exceptions thrown from a test
  (or any hook methods).

## Migration notes (from randomizedtesting for junit4)

* `RandomizedContext.getRunnerSeedAsString()` is replaced with a `getRootSeed()` method
  on any injected `RandomizedContext` object.
