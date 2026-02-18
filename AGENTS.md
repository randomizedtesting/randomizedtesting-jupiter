# AGENTS.md

This project is written by humans, however LLMs have become a lot more useful in speeding up
coding tasks and this file contains a condensed knowledge on the codebase and guidelines for coding agents.

See https://agents.md for more info and how to make various coding assistants consume this file.

## Licensing and Dependencies

- All module dependencies must be declared in `gradle/libs.versions.toml`, never inside build.gradle files.

## Build and Development Workflow

- When done or preparing to commit changes to java source files, be sure to run `gradlew tidy` to format the code.
- Always run `gradlew check` before declaring a feature done.

## Code Quality and Best Practices

- Assume you're an expert programmer. Only add comments where the code flow does not explain what's happening.
- Assume minimum version of Java is 21. Use the features available in that Java version.

## Testing

- When adding a test to an existing suite/file, keep the same style / design choices.
- Do not add excessive tests. Keep them minimal.

## Documentation

- Keep all documentation including javadoc concise.
- New classes should have some javadocs.
