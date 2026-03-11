# Feature: detecting tests that "leak" threads

## Functionality

* It should be possible to add a `@DetectThreadLeaks` extension which detects new threads forked within the test
  container. This extension takes a parameter - the scope of detection. Either we care about threads leaked
  from the entire container or from each individual test. Here is an example of use:

```java

@DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
public class TestClass {
    @Test
    public void testMethod() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }).start();
    }
}
```

* The extension is **only functional in sequential mode**. It should emit a warning and do nothing if tests are
  run in concurrent mode.

* Occasionally there will be threads that cannot be joined but will eventually terminate. One can specify an additional
  "linger" time before the thread leak is reported, for example one second, below:

```java

@DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
@DetectThreadLeaks.LingerTime(millis = 1_000)
public class TestClass {
    @Test
    public void testMethod() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }).start();
    }
}
```

* In certain cases, system threads or other threads beyond the test's control may be started and cannot be terminated
  within the test's scope. The `@DetectThreadLeaks.ExcludeThreads` annotation can provide programmatic filters which
  tell the extension to ignore certain threads.

* When `@DetectThreadLeaks` is active, it also detects **uncaught exceptions** thrown by any thread
  during the scope. Such exceptions are collected and reported as test failures. If both a thread leak and uncaught
  exceptions occur, all are reported: the leak error is thrown and the
  uncaught-exception errors are attached as suppressed exceptions.

```java

@DetectThreadLeaks(scope = DetectThreadLeaks.Scope.TEST)
public class TestClass {
    @Test
    public void testMethod() throws InterruptedException {
        Thread t = new Thread(() -> {
            throw new RuntimeException("background failure");
        });
        t.start();
        t.join();
    }
}
```

## Migration notes (from randomizedtesting for junit4)

* `@ThreadLeakScope` is replaced with `@DetectThreadLeaks(scope = ...)`. The `NONE` scope
  (disabling all checks) maps to `@DetectThreadLeaks(scope = DetectThreadLeaks.Scope.NONE)`,
  or simply remove `@DetectThreadLeaks` from the class entirely.
  The default scope changed: the old default was `TEST`; the new default is `SUITE`.

* `@ThreadLeakLingering(linger = N)` is replaced with `@DetectThreadLeaks.LingerTime(millis = N)`.
  The annotation can now be placed on individual test methods as well as on the class, with the
  method-level value taking precedence.

* `@ThreadLeakFilters(filters = {MyFilter.class})`: replace with
  `@DetectThreadLeaks.ExcludeThreads(MyFilter.class)`. The filter interface changed from
  `ThreadFilter.reject(Thread)` (return `true` to exclude) to `Predicate<Thread>.test(Thread)`
  (return `true` to exclude). Rename and invert the logic accordingly. The `defaultFilters`
  flag has no equivalent; built-in system-thread filters are always applied. Filters are now
  collected hierarchically from the method, the class, and all superclasses, and combined with OR.

* `@ThreadLeakAction`: interrupt-on-leak is now always performed as cleanup (no annotation
  needed). Leaked threads are interrupted and joined before the failure is reported. There is no
  equivalent of the `WARN`-only mode.

* `@ThreadLeakZombies`: zombie tracking (marking remaining tests as aborted when a leaked
  thread could not be killed) is not implemented. Threads that survive the interrupt/join budget
  are still reported in the failure message but subsequent tests are not skipped.

* `@ThreadLeakGroup` — the group scope (`ALL` / `MAIN` / `TESTGROUP`) is not configurable.
  The extension always uses `Thread.getAllStackTraces()`, equivalent to the old `ALL` group, and
  filters out known system threads automatically.
