# Migrating from JUnit 4 to JUnit 6 with RandomizedTesting

This guide covers all the changes required to migrate a project that uses
[RandomizedTesting](https://github.com/randomizedtesting/randomizedtesting)
from the JUnit 4 integration (`randomizedtesting-runner`) to the JUnit Jupiter
integration (`randomizedtesting-jupiter`).

---

## 1. Maven dependencies

### 1.1 Add the JUnit BOM

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

### 1.2 Replace the JUnit dependency

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

### 1.3 Replace the RandomizedTesting dependency

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

### 1.4 Simplify the build plugin configuration

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

---

## 2. Test runner and class-level annotations

### 2.1 Replace `@RunWith` with `@Randomized`

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

### 2.2 Replace `@ThreadLeakFilters` with `@DetectThreadLeaks`

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

### 2.3 Update the thread-leak filter interface

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

---

## 3. Lifecycle annotations

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

---

## 4. Accessing the test name

### 4.1 Replace `@Rule TestName` with `TestInfo`

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
@BeforeEach
void beforeEach(TestInfo testInfo) {
    System.out.println("Running: " + testInfo.getDisplayName());
}

@Test
void myTest() { ... }
```

---

## 5. Randomness

### 5.1 Inject `Random` explicitly

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

### 5.2 Replace `@Seed` with `@FixSeed`

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

---

## 6. Repeated tests

JUnit 4 used the RandomizedTesting-specific `@Repeat` annotation combined with
`@Test`. JUnit Jupiter provides a built-in `@RepeatedTest` annotation that
replaces both.

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

---

## 7. Class and method visibility

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

---

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
