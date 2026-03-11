# Migration notes: features removed or replaced by junit5 functionality

The following features of junit4-compatible `RandomizedRunner` have been
removed or replaced by built-in functionality in junit5.

## Parameterized tests (`@ParametersFactory`)

* This feature is already part of JUnit5: use `@ParameterizedClass`, `@ParameterizedTest`
  or any other way to create dynamic tests that JUnit5 offers.

## Test groups and test filtering

* This feature is already part of JUnit5 in the
  form of[`@Tag` annotations](https://docs.junit.org/6.0.3/writing-tests/tagging-and-filtering.html).

* Direct use of `tests.filter` and `tests.[group-name]` properties
  is not as straightforward as it was with `RandomizedRunner`. These properties
  need to be converted and passed to JUnit Jupiter using your build system's
  facilities: system properties alone won't have any effect. Here is
  an [example for gradle](https://docs.gradle.org/current/userguide/java_testing.html#test_grouping).

## Custom test case order (`@TestCaseOrdering`)

* This feature is already part of JUnit5 (`@TestMethodOrder`). However,
  It is technically not possible to write a reorderer that will use the root
  seed value to reorder methods consistently (because method orderers
  are run in discovery phase and execution contexts are not available then).

## Test instance creation control (`@TestCaseInstanceProvider`).

* This feature is already part of JUnit5 (`@TestInstance`).

## Custom event listeners (`@Listeners`).

* This feature is already part of JUnit5 (`TestExecutionListener`).

## Custom test-method providers (`@TestMethodProviders()`)

* JUnit5/ Jupiter offers many ways to discover tests dynamically. `@TestFactory`
  methods, templates, etc. While there is no one-to-one replacement, it should
  be possible to reimplement custom test providers with some minor refactorings.
  For example, if a `TestMethodProvider` was including all `test*` methods (JUnit3-style),
  you could write a `@TestFactory` in a common superclass and then just extend it. This
  test factory would something like this:

```java

@TestFactory
Stream<DynamicTest> includeTestMethodsWithNoAnnotations() {
    return Arrays.stream(getClass().getDeclaredMethods())
            .filter(m -> m.getName().startsWith("test")
                         && m.getParameterCount() == 0
                         && !Modifier.isStatic(m.getModifiers()))
            .map(m -> DynamicTest.dynamicTest(m.getName(), () -> {
                m.invoke(this);
            }));
}
```

## Repeating tests with @Repeat

* Use standard JUnit5 test repetition facilities (like test templates, or `@RepeatedTest`
  annotation). To repeat the same test with a constant seed, `@FixSeed` at class or test level.
  If the seed is not fixed, it will be different for each test repetition by default.

## Multiple extensions instead of a single `RandomizedRunner`

* The `RandomizedRunner` combined multiple features in one class. This has been replaced by multiple
  extensions, which can be used independently. So `@Randomized` and `@DetectThreadLeaks` can be used
  completely independently of each other, for example.
