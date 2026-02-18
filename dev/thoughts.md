# randomized context: creation, hierarchical context, access from forked threads, etc.  

Implement test randomization context passing and seed hierarchy/ aggregation/ exception injection reporting.

// TODO: test filtering of a repeated test (from the same root seed) should rerun the same test.
// TODO: stack trace augmentation in case of failed tests (seed).

* randomized context supplier should be initialized via service loader extension (so that it is always initialized)
* potential problem - implementing @Repeat and @Seed(s); conflicts with other template test extensions in JUnit5. 
TestTemplateInvocationContextProvider is method-based; can't declare a @Randomized annotation on a class and blow up
tests for different seeds (like -Dtests.repeat=10 previously). Class templates are preview features but they always add
context test nodes and this looks odd. It'd also conflict with other jupiter features. 
* stable method ordering (depending on the seed) may be hard to achieve without explicit custom orderers.
* running tests in parallel will conflict with thread leak detection (can't identify test group leaks; tests running under fj pool).
* can't easily tell test seed from uniqueid - these use template invocation counts in their node ids:
```
[engine:junit-jupiter]/[class:com.carrotsearch.randomizedtesting.TestSeeds$NestedWithSeeds]/[test-template:testWithFixedSeeds()]/[test-template-invocation:#1]
```
