# randomized context: creation, hierarchical context, access from forked threads, etc.  

Implement test randomization context passing and seed hierarchy/ aggregation/ exception injection reporting.

* support basic context injection.
```java
@ExtendWith({@RandomizedContextSupplier})
class Foo { 
    @Test
    void seedViaParameter(RandomizedContext ctx) {
      assertNotNull(ctx);
    }
}
```

* support current-thread context access (?)
```java
@Test
void seedViaThreadLocal() {
    assertNotNull(RandomizedContext.current());
}
```

* support parameterized tests and classes?
```java
@Randomized
@ParameterizedTest
@ValueSource(strings = { "a", "b", "c" })
void palindromes(String candidate, RandomizedContext ctx) {
  assertNotNull(ctx);
}
```

* randomized context supplier should be initialized via service loader extension (so that it is always initialized)
* potential problem - implementing @Repeat and @Seed(s); conflicts with other template test extensions in JUnit5 (only
one TestTemplateInvocationContextProvider is allowed on a class).
