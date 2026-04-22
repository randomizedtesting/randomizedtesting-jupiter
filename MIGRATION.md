# Migrating from JUnit 4 to JUnit 6 with RandomizedTesting

This guide covers all the changes required to migrate a project that uses
[RandomizedTesting](https://github.com/randomizedtesting/randomizedtesting)
from the JUnit 4 integration (`randomizedtesting-runner`) to the JUnit Jupiter
integration (`randomizedtesting-jupiter`).

## Maven dependencies

### Add the JUnit BOM

Add a `dependencyManagement` section to import the JUnit BOM. This centralises
version management for all JUnit artifacts so you only need to declare versions
in one place.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.junit</groupId>
            <artifactId>junit-bom</artifactId>
            <version>6.0.3</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

### Replace the JUnit dependency

| Before                           | After                                                |
| -------------------------------- | ---------------------------------------------------- |
| `junit:junit` (explicit version) | `org.junit.jupiter:junit-jupiter` (version from BOM) |

**Before:**

```xml
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.2</version>
    <scope>test</scope>
</dependency>
```

**After:**

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Replace the RandomizedTesting dependency

| Before                                                        | After                                                          |
| ------------------------------------------------------------- | -------------------------------------------------------------- |
| `com.carrotsearch.randomizedtesting:randomizedtesting-runner` | `com.carrotsearch.randomizedtesting:randomizedtesting-jupiter` |

**Before:**

```xml
<dependency>
    <groupId>com.carrotsearch.randomizedtesting</groupId>
    <artifactId>randomizedtesting-runner</artifactId>
    <version>2.8.4</version>
    <scope>test</scope>
</dependency>
```

**After:**

```xml
<dependency>
    <groupId>com.carrotsearch.randomizedtesting</groupId>
    <artifactId>randomizedtesting-jupiter</artifactId>
    <version>0.2.0</version>
    <scope>test</scope>
</dependency>
```

### Simplify the build plugin configuration

JUnit 4 required disabling the default Surefire execution and replacing it with
the dedicated `junit4-maven-plugin`. With JUnit Jupiter, standard Maven Surefire
natively discovers and runs tests — no custom plugin wiring is needed.

**Before:**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.5</version>
    <executions>
        <execution>
            <id>default-test</id>
            <phase>none</phase>
        </execution>
    </executions>
</plugin>

<plugin>
<groupId>com.carrotsearch.randomizedtesting</groupId>
<artifactId>junit4-maven-plugin</artifactId>
<version>2.8.4</version>
<executions>
    <execution>
        <id>unit-tests</id>
        <phase>test</phase>
        <goals><goal>junit4</goal></goals>
    </execution>
</executions>
</plugin>
```

**After:**

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.5</version>
</plugin>
```

## Test runner and class-level annotations

### Replace `@RunWith` with `@Randomized`

`@RunWith(RandomizedRunner.class)` is the JUnit 4 mechanism for plugging in a
custom runner. In JUnit Jupiter it is replaced by the `@Randomized` extension
annotation from `randomizedtesting-jupiter`.

**Before:**

```java
@RunWith(RandomizedRunner.class)
public class MyTest { ... }
```

**After:**

```java
@Randomized
class MyTest { ... }
```

### Replace `@ThreadLeakFilters` with `@DetectThreadLeaks`

Thread-leak detection is now configured with two separate annotations:
`@DetectThreadLeaks` to enable detection (now disabled by default), and `@DetectThreadLeaks.ExcludeThreads`
to whitelist known background threads.

**Before:**

```java
@ThreadLeakFilters(filters = {MyTest.MyFilter.class})
public class MyTest { ... }
```

**After:**

```java
@DetectThreadLeaks
@DetectThreadLeaks.ExcludeThreads(MyTest.MyFilter.class)
@Randomized
class MyTest { ... }
```

### Update the thread-leak filter interface

The `ThreadFilter` interface is replaced by the standard Java `Predicate<Thread>`.
Note that **the contract is inverted**: `ThreadFilter.reject(t)` returned `true`
to _ignore_ a thread, while `Predicate.test(t)` returns `true` to _exclude_ it
from leak detection (same intent, opposite boolean sense).

| Before                                 | After                                   |
| -------------------------------------- | --------------------------------------- |
| `implements ThreadFilter`              | `implements Predicate<Thread>`          |
| `public boolean reject(Thread t)`      | `public boolean test(Thread t)`         |
| returns `true` → thread is **ignored** | returns `true` → thread is **excluded** |

**Before:**

```java
public static class MyFilter implements ThreadFilter {
    public boolean reject(Thread t) {
        return "my-background-thread".equals(t.getName());
    }
}
```

**After:**

```java
public static class MyFilter implements Predicate<Thread> {
    public boolean test(Thread t) {
        return "my-background-thread".equals(t.getName());
    }
}
```

## Randomness

### Inject `Random` explicitly

The JUnit 4 integration exposed randomness through static no-arg helper methods
(typically by extending `RandomizedTest` or using static imports). The Jupiter
integration instead injects a `Random` instance as a method parameter. This makes
the seed traceable per-invocation and removes the hidden dependency on a base class.

The `Random rnd` parameter can be declared on `@Test`, `@BeforeAll`, `@BeforeEach`,
or any other lifecycle method that needs randomness.

| Before                       | After                             |
| ---------------------------- | --------------------------------- |
| `randomInt()`                | `randomInt(rnd)`                  |
| `randomIntBetween(min, max)` | `randomIntInRange(rnd, min, max)` |
| `randomBoolean()`            | `randomBoolean(rnd)`              |
| `randomLocale()`             | `randomLocale(rnd)`               |

**Before:**

```java
@Test
public void myTest() {
    int n = randomInt();
}
```

**After:**

```java
@Test
void myTest(Random rnd) {
    int n = randomInt(rnd);
}
```

### Replace `@Seed` with `@FixSeed`

| Before           | After               |
| ---------------- | ------------------- |
| `@Seed("value")` | `@FixSeed("value")` |

**Before:**

```java
@Test
@Seed("DEADBEEF")
public void myTest() { ... }
```

**After:**

```java
@Test
@FixSeed("DEADBEEF")
void myTest(Random rnd) { ... }
```

## Repeated tests

JUnit 4 used the RandomizedTesting-specific `@Repeat` annotation combined with
`@Test`. JUnit Jupiter provides a built-in `@RepeatedTest` annotation that
could replace both on a method level.

But note that when using `@RepeatedTest`, you can not add a fixed seed with `@FixSeed` on the method
as the same seed will be used for all the repeated tests which defeats the purpose.

Instead, you should use the `-Dtests.iters=5` system property to control the number of iterations and let each
iteration use a different random seed or set the root seed with `-Dtests.seed=12345`.

**Before:**

```java
@Test
@Repeat(iterations = 5)
public void myTest() { ... }
```

**After:**

```java
@RepeatedTest(5)
void myTest(Random rnd) { ... }
```

## JUnit 4 to Junit Jupiter related changes

### Class and method visibility

JUnit 4 required test classes and methods to be `public`. JUnit Jupiter does not —
package-private is the recommended convention.

**Before:**

```java
public class MyTest {
    @Test
    public void myTest() { ... }
}
```

**After:**

```java
class MyTest {
    @Test
    void myTest() { ... }
}
```

### Lifecycle annotations

All JUnit 4 lifecycle annotations have direct replacements in JUnit Jupiter.
Methods no longer need to be `public`.

| JUnit 4        | JUnit Jupiter |
| -------------- | ------------- |
| `@BeforeClass` | `@BeforeAll`  |
| `@AfterClass`  | `@AfterAll`   |
| `@Before`      | `@BeforeEach` |
| `@After`       | `@AfterEach`  |

**Before:**

```java
@BeforeClass
public static void setUp() { ... }

@AfterClass
public static void tearDown() { ... }
```

**After:**

```java
@BeforeAll
static void setUp() { ... }

@AfterAll
static void tearDown() { ... }
```

### Replace `@Rule TestName` with `TestInfo`

JUnit 4 exposed the current test name through a `@Rule` field. In JUnit Jupiter,
`TestInfo` is injected as a parameter into any lifecycle method or test method.
The most common pattern is a single `@BeforeEach` method that receives `TestInfo`,
which avoids repeating the same lookup in every test.

**Before:**

```java
@Rule
public TestName name = new TestName();

@Test
public void myTest() {
    System.out.println("Running: " + name.getMethodName());
}
```

**After:**

```java
@Test
void myTest(TestInfo testInfo) {
    System.out.println("Running: " + testInfo.getDisplayName());
}
```

## Summary

| Concern                 | JUnit 4                                      | JUnit Jupiter                                              |
| ----------------------- | -------------------------------------------- | ---------------------------------------------------------- |
| Test runner             | `@RunWith(RandomizedRunner.class)`           | `@Randomized`                                              |
| Thread leak detection   | `@ThreadLeakFilters(filters = {...})`        | `@DetectThreadLeaks` + `@DetectThreadLeaks.ExcludeThreads` |
| Thread filter contract  | `ThreadFilter.reject()` → `true` to ignore   | `Predicate<Thread>.test()` → `true` to exclude             |
| Maven test execution    | Custom `junit4-maven-plugin`                 | Standard Maven Surefire                                    |
| Randomized helpers      | Static no-arg methods (base class or import) | Static methods with explicit `Random rnd` parameter        |
| Seeded test             | `@Seed("value")`                             | `@FixSeed("value")`                                        |
| Repeated test           | `@Test` + `@Repeat(iterations = N)`          | `@RepeatedTest(N)`                                         |
| Test lifecycle (class)  | `@BeforeClass` / `@AfterClass`               | `@BeforeAll` / `@AfterAll`                                 |
| Test lifecycle (method) | `@Before` / `@After`                         | `@BeforeEach` / `@AfterEach`                               |
| Test name access        | `@Rule TestName` field                       | `TestInfo` parameter injection                             |
| Class/method visibility | `public` required                            | Package-private recommended                                |

## See also

The [com.carrotsearch.randomizedtesting.tests](randomizedtesting-jupiter/src/test/java/com/carrotsearch/randomizedtesting/tests)
package contains tests that demonstrate the features of `randomizedtesting-jupiter` and their migration from JUnit 4.
A Markdown file is available within the same dir to explain the main differences with the JUnit 4
implementation:

- [F001 — RandomizedContext injection](randomizedtesting-jupiter/src/test/java/com/carrotsearch/randomizedtesting/tests/F001_RandomizedContextInjection.md): how to inject `RandomizedContext` into test methods and lifecycle hooks to access seeded randomness.
- [F002 — Seed recovery](randomizedtesting-jupiter/src/test/java/com/carrotsearch/randomizedtesting/tests/F002_SeedRecovery.md): how to retrieve the root seed after a failure so a flaky test can be reproduced exactly.
- [F003 — Random injection](randomizedtesting-jupiter/src/test/java/com/carrotsearch/randomizedtesting/tests/F003_RandomInjection.md): how to inject a `Random` instance (or a `Supplier<Random>`) directly into test methods, including alternative implementations.
- [F004 — Seed fixing](randomizedtesting-jupiter/src/test/java/com/carrotsearch/randomizedtesting/tests/F004_SeedFixing.md): how to pin a constant seed at class or method level with `@FixSeed` to reproduce failures deterministically.
- [F005 — Thread leak detection](randomizedtesting-jupiter/src/test/java/com/carrotsearch/randomizedtesting/tests/F005_ThreadLeaks.md): how to detect threads leaked by tests and capture uncaught background exceptions with `@DetectThreadLeaks`.
- [F006 — RandomizedTest base class](randomizedtesting-jupiter/src/test/java/com/carrotsearch/randomizedtesting/tests/F006_RandomizedTest.md): overview of the `RandomizedTest` convenience superclass and what changed compared to its JUnit 4 counterpart.
- [F007 — Test reiteration](randomizedtesting-jupiter/src/test/java/com/carrotsearch/randomizedtesting/tests/F007_TestReiteration.md): how to re-run tests multiple times with the same or varying seed via the `tests.iters` system property.
- [F999 — Removed features](randomizedtesting-jupiter/src/test/java/com/carrotsearch/randomizedtesting/tests/F999_RemovedFeatures.md): features from `RandomizedRunner` that are dropped or replaced by built-in JUnit Jupiter equivalents (`@Tag`, `@TempDir`, `@TestMethodOrder`, etc.).
