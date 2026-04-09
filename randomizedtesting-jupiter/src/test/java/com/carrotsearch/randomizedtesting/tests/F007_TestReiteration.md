# Feature: re-run (reiterate) one or more tests multiple times, with constant or varying seeds.

## Functionality

* It should be possible to "rerun" one or more tests multiple times with the same or varying seed to check if a
  problematic
  failure depends on the seed (reproduces) or if it's not reproducible.

* The reiteration is controlled by a system property `tests.iters`, taking the number of reiterations to execute.

* Only junit jupiter tests are reiterated at the moment (by wrapping jupiter test engine and delegating execution to
  it).

* If `tests.seed` is fixed, all reiterations should result in the same randomness and execution results. Otherwise,
  each reiteration starts with a random root seed.

## Migration notes (from randomizedtesting for junit4)

* Test reiteration will run the full stack of all extensions and hooks - it's not a mere re-execution of an
  individual test. 
