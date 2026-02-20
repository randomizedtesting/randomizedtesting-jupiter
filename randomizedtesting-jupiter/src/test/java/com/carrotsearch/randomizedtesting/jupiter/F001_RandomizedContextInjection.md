# Feature: injection of RandomizedContext

## Functionality

* It should be possible to get access to a `RandomizedContext` object via parameters
  injected into test methods and hooks. For example,

```java

@Randomized
public class TestClass {
    @Test
    public void testMethod(RandomizedContext ctx) {
    }
}
```

* The injected `RandomizedContext` provides access to a `Random` instance for that
  context and uses context-specific seed, derived from parent contexts.
  So, the initial seed of the root context affects derived, nested contexts (before-all,
  before-test, tests, etc.). This repeated test would fail 50% of the time (predictably,
  given the same root seed):

```java

@Randomized
public class TestClass {
    @RepeatedTest(10)
    public void testMethod(RandomizedContext ctx) {
        assert ctx.getRandom().nextBoolean();
    }
}
```

* `RandomizedContext` instances are pre-seeded for reproducibility. This means running the
same set of tests from the same value of the "root seed" (and assuming sequential,
predictable test code) must result in the same values returned from the injected `Random`.

* Injecting `RandomizedContext` must be compatible with test templates, class
  templates and other types of junit jupiter tests.

* The root seed can be provided via system property `tests.seed`. The format of this seed
  is internal to the implementation but hierarchical: the initial seed for context-specific
  `RandomizedContext` can be "pinned" to rerun the same test with the same randomness multiple
  times. For example, running the `TestClass` above with a fixed seed `deadbeef:cafebabe` would
  result in 10 repetitions of `testMethod` _with the same initial random value_.

## Migration notes (from randomizedtesting for junit4)

* `@RunWith(RandomizedRunner.class)` is no longer available. Use `@Randomized`
  on the class or test method where randomization should be available.

* Since we don't use a custom runner, support for non-public lifecycle
  methods (`@Before`, `@After`, etc.) is no longer available.
