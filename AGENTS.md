# Repository Guidelines

## Project Structure & Module Organization
The Maven root `pom.xml` aggregates server components under `components/` (e.g., `components/org.wso2.carbon.identity.recovery`) and packaging features under `features/`. Java sources live in `src/main/java` within each module, with tests in `src/test/java`. Shared docs (e.g., `README.md`) sit at the repo root.

## Build, Test, and Development Commands
- `mvn clean install` from the repo root compiles all modules, runs unit/integration tests, and assembles feature bundles.
- `mvn -pl components/<module> clean test` targets a specific component when isolating failures.
- `mvn dependency:tree` helps diagnose classpath conflicts across the multi-module build.

## Coding Style & Naming Conventions
Follow the default Maven Checkstyle profile configured per module (Sun-based with 4-space indentation). Use PascalCase for classes, camelCase for methods/fields, and uppercase snake case for constants. Keep package names lowercase and dot-separated under `org.wso2.carbon.identity`. Run `mvn checkstyle:check` before pushing.

## Testing Guidelines
Unit tests use TestNG; integration tests run where defined, with JaCoCo tracking coverage thresholds on complexity ratio. Name test classes `<ClassName>Test` and place shared fixtures under `src/test/resources`. Execute `mvn clean verify` to ensure all checks, coverage rules, and SpotBugs run.

## Commit & Pull Request Guidelines
Craft commits in present tense (e.g., "Fix recovery workflow null check") and keep them scoped to a logical change. Reference GitHub issues with `Resolves #<id>` when applicable. Pull requests should fill out `pull_request_template.md`, include test evidence, note any migration impacts, and confirm secure coding and secret scanning checkboxes.

## Security & Configuration Tips
Never commit credentials or tenant configs; use placeholders in `deployment.toml`. Run `mvn spotbugs:check` and follow WSO2 Secure Engineering guidelines before submission.
