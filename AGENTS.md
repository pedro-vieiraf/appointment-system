# Appointment System

This repository is a Java backend learning and portfolio project. It provides a REST API
for authenticated users to manage their own appointments. The goal is to develop skills
the maintainer can explain and apply independently, including API design, Spring Security,
persistence, testing, and architectural tradeoffs.

## Working with the maintainer

- Discuss the requirement, the layer that owns the behavior, and the proposed design
  before implementing. Explain the Java or Spring mechanisms involved when they matter.
- Use small TDD increments when changing behavior. Write a test that demonstrates the
  requirement, implement the minimum behavior, run the tests, and refactor as needed.
  Give the maintainer room to reason about or implement the next step.
- Discuss substantial architectural changes and new dependencies with the maintainer.
  Keep the layered monolith and add infrastructure when a use case or clear learning
  objective justifies it.
- Use English for source code, identifiers, and new technical documentation. Use the
  maintainer's language in conversation.

## Architecture and conventions

- Java 21, Spring Boot, Spring MVC, Spring Security, Spring Data JPA, PostgreSQL, and
  Maven. Read the current dependency versions in `pom.xml`.
- Controllers map HTTP requests and responses using DTOs. Services orchestrate use cases,
  repositories handle persistence, and entities enforce domain rules. The global
  exception handler maps application exceptions to HTTP responses.
- Derive appointment ownership from the authenticated user. Scope reads and writes to
  that user. Keep list filters in the existing JPA Specification.
- Appointments start as `PENDING`. They can move to `CONFIRMED` or `CANCELLED`.
  Confirmed appointments can move to `CANCELLED` or `COMPLETED`. Use the entity's
  transition methods and preserve the existing HTTP 409 response for invalid transitions.
- Follow the existing package structure, validation, and test conventions. Check the
  controller and its tests when documenting an HTTP contract.

## Build and validation

- Run `./mvnw test` for tests or `./mvnw verify` for the build and tests used by CI.
- Run `./mvnw checkstyle:check` for style validation.
- Full-context tests expect PostgreSQL. Check test properties and the CI workflow for
  database settings, and point tests at a dedicated test database. In restricted
  environments, verify PostgreSQL availability and the Maven cache before relying on
  Docker or dependency downloads.

## Git and release workflow

- Check the current branch and worktree before editing. Preserve existing changes.
- Start feature branches from `develop` and follow the existing
  `feature/<short-description>` convention. Keep changes focused on the branch purpose.
- The v0.5.0 roadmap starts with OpenAPI and Swagger UI, followed by a JaCoCo coverage
  baseline. Check the current branch before assuming which work has already landed.
