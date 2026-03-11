# Feature: seed fixing using `FixSeed` annotation

## Functionality

* It should be possible to "fix" (make constant, regardless of the
  randomization state) the random seed for methods and classes. This can be achieved using `@FixSeed` annotation

```java

@Randomized
@FixSeed("cafebabe")
public class TestClass {
    @Test
    public void testMethod(Random ctx) {
    }
}
```

it is also possible to fix the seed for a particular method, although this allows state randomization at
class level:

```java

@Randomized
public class TestClass {
    @Test
    @FixSeed("cafebabe")
    public void testMethod(Random ctx) {
    }
}
```

* The value of the `@FixSeed` annotation can be a single seed or a chain of seeds, affecting nested contexts.

```java

@Randomized
@FixSeed("cafebabe:deadbeef")
public class TestClass {
    @Test
    public void testMethod(Random ctx) {
    }
}
```

* `@FixSeed` can be used to rerun the same tests multiple times with a constant seed or predictably varying seed. For
  example, this test runs 5 times with the same seed/ randomness at the test level:

```java

@Randomized
public class TestClass {
    @RepeatedTest(5)
    @FixSeed("babe")
    public void testMethod(Random ctx) {
    }
}
```

but this test runs 5 times, each time with a different (but predictable, derived from the parent) seed:

```java

@Randomized
@FixSeed("babe")
public class TestClass {
    @RepeatedTest(5)
    public void testMethod(Random ctx) {
    }
}
```

## Migration notes (from randomizedtesting for junit4)

* `@FixSeed` is renamed from the `@Seed` annotation, used previously.

* There is no way to provide multiple seeds (`@Seeds` annotation), there is no replacement for this functionality.

* There are subtle differences in how the annotation propagates but overall think of the seed annotation placement
(method, class) as affecting the corresponding JUnit5 extension context (its path in the test's UniqueId).
