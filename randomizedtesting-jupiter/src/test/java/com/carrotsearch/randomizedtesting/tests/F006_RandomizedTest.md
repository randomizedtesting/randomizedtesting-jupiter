# Feature: the `RandomizedTest` parent infrastructure class

## Functionality

* This parent class adds convenience methods to other utility classes and combines several bits of infrastructure
  together (like thread leak detection and test multiplier handling).

## Migration notes (from randomizedtesting for junit4)

* All convenience methods have an explicit `Random` parameter. No thread-local context is present so the random needs to
  be passed explicitly from the calling method.

* Some methods have been removed - for example temporary file/ directory creation, which is provided by JUnit5 Jupiter by
  default using its `@TempDir` annotation.

* "Nightly mode" awareness has been removed since test groups no longer exist.
