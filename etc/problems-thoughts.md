* potential problem - implementing @Repeat and @Seed(s); conflicts with other template test extensions in JUnit5. 
TestTemplateInvocationContextProvider is method-based; can't declare a @Randomized annotation on a class and blow up
tests for different seeds (like -Dtests.repeat=10 previously). Class templates are preview features but they always add
context test nodes and this looks odd. It'd also conflict with other jupiter features. 

* running tests in parallel will conflict with thread leak detection (can't identify test group leaks; tests running under fj pool).
