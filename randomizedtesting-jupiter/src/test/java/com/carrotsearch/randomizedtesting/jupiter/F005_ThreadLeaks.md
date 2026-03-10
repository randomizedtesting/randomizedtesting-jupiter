# Feature: detecting tests that "leak" threads  

## Functionality

* It should be possible to add a `@DetectThreadLeaks` extension which detects new threads forked within the test 
container. This extension takes a single parameter - the scope of detection. Either we care about threads leaked
from the entire container or from each individual test. Here is an example of use:

```java

@DetectThreadLeaks(scope = DetectThreadLeaks.Scope.SUITE)
public class TestClass {
    @Test
    public void testMethod() {
        new Thread(() -> {
            try { Thread.sleep(1000); } catch (Exception e) {}
        }).start();
    }
}
```

* The extension is only functional in sequential mode. It should emit a warning and do nothing if tests are
run in concurrent mode.

## Migration notes (from randomizedtesting for junit4)

* 
