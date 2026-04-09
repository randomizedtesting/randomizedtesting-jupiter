# Randomized Testing for JUnit5 (Jupiter Engine)

A set of JUnit5 (Jupiter Engine) extensions for more controlled
pseudo-randomness in unit tests.

This is a rewrite of the JUnit4-based `randomizedtesting` project.

See [the test package](randomizedtesting-jupiter/src/test/java/com/carrotsearch/randomizedtesting/tests)
for current features, their requirement descriptions and migration from junit4.

See [LICENSE.txt](LICENSE.txt) to make your company's lawyer happy.

## Snapshot artifacts and releases

We do not publish snapshot artifacts. If you'd like to work with a snapshot,
use gradle's composite build or install maven artifacts locally with:

```
./gradlew publishToMavenLocal
```

## Release publishing

```
./gradlew publishToSonatype closeSonatypeStagingRepository
```