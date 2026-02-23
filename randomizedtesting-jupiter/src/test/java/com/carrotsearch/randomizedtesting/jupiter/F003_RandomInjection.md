# Feature: direct injection of Random instances

## Functionality

* It should be possible to get access to a `Random` object via parameters
  injected into test methods and hooks. For example,

```java

@Randomized
public class TestClass {
    @Test
    public void testMethod(Random ctx) {
    }
}
```

The injected `Random` is initialized with the context's seed.

## Migration notes (from randomizedtesting for junit4)

This is new functionality, it wasn't available before.
