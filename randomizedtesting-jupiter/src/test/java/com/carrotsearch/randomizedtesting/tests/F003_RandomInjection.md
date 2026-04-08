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

* It should be possible to pick (via system properties or JUnit5 configuration
  parameters) different `Random` implementations for the
  injected parameter. Lockless or those providing larger state space than
  the default `java.util.Random`.

* The injected `Random` is tied to the thread that created it. When
  assertions are enabled (or an explicit parameter is set), the injected
  Random instances should verify they are indeed used from within the
  right thread.

* It should be possible to inject a parameter of type `Supplier<Random>`. This
  supplier is safe to use from any thread; it returns a `Random` instance initialized
  with the same starting seed if called from a previously unseen thread.

## Migration notes (from randomizedtesting for junit4)

This is new functionality, it wasn't available before.
