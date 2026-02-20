# Migration notes: features removed or replaced by junit5 functionality

The following features of junit4-compatible `RandomizedRunner` have been 
removed or replaced by built-in functionality in junit5.

## Parameterized tests (`@ParametersFactory`)

* This feature is already part of JUnit5: use `@ParameterizedClass`, `@ParameterizedTest`
or any other way to create dynamic tests that JUnit5 offers.

## Test groups

* This feature is already part of JUnit5 (as `@Tag` annotations).

## Custom test case order (`@TestCaseOrdering`)

* This feature is already part of JUnit5 (`@TestMethodOrder`). However,
It is technically not possible to write a reorderer that will use the root
seed value to reorder methods consistently (because method orderers
are run in discovery phase and execution contexts are not available then).

## Test instance creation control (`@TestCaseInstanceProvider`).

* This feature is already part of JUnit5 (`@TestInstance`).

## Custom event listeners (`@Listeners`).

* This feature is already part of JUnit5 (`TestExecutionListener`).
