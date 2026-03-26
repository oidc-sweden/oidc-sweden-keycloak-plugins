# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A Keycloak 26.x plugin implementing the [Swedish OIDC Claims and Scopes Specification 1.0](https://www.oidc.se/specifications/swedish-oidc-claims-specification.html). It registers Swedish-specific OIDC scopes, protocol mappers, user profile attributes, and a realm resource provider into Keycloak via its SPI mechanism.

## Build and test

```bash
# Build (skip tests)
mvn -DskipTests clean package

# Full build with tests
mvn clean package

# Run a single test class
mvn -Dtest=MyTestClass test

# Run a single test method
mvn -Dtest=MyTestClass#myMethod test
```

Output JAR: `target/oidc-sweden-claims-plugin-*.jar`

## Architecture

The plugin is structured around Keycloak's SPI extension points. SPIs are registered in `src/main/resources/META-INF/services/`.

### Entry points

| Class | SPI | Description |
|-------|-----|-------------|
| `OidcSwedenRealmResourceProviderFactory` | `RealmResourceProviderFactory` | Runs `postInit()` at Keycloak startup to set up all existing realms; also exposes `GET /realms/{realm}/oidc-sweden/info` |
| `OidcSwedenEventListenerProviderFactory` | `EventListenerProviderFactory` | Listens for `REALM_CREATE` admin events to set up newly created realms |
| `SwedishOidcClaimsMapper` | `ProtocolMapper` | Maps Swedish OIDC identity claims to tokens |
| `NaturalPersonInfoMapper` | `ProtocolMapper` | Maps `naturalPersonInfo` scope claims to standard OIDC profile claims |

### Realm setup (idempotent)

`OidcSwedenRealmSetup` is the shared setup logic called from both the factory `postInit()` (existing realms) and the event listener (new realms). It is safe to call multiple times — both `ensureAttributes()` and `ensureScopes()` only add what is missing.

### Scopes, mappers, and attributes

- `SwedishOidcScopes` defines the three Swedish OIDC client scopes and which mapper to attach to each.
- `SwedishOidcAttributes` defines 11 persistent user profile attributes (identity, org, personal).
- The two protocol mappers read these user attributes and Keycloak's built-in `firstName`/`lastName` fields to produce JWT claims.

## Key constants

- Scope URIs (e.g. `https://id.oidc.se/scope/naturalPersonNumber`) live in `SwedishOidcScopes`.
- Claim names and attribute keys live in `SwedishOidcAttributes`.
- Provider IDs: `swedish-oidc-claims-mapper`, `natural-person-info-mapper`, `oidc-sweden-event-listener`, `oidc-sweden`.

## Dependencies

- **Keycloak 26.x** and **Jackson** are `provided` scope — bundled by Keycloak at runtime, not in the JAR.
- Tests use JUnit 5 and Mockito 5.

## Installation

1. Copy JAR to `/opt/keycloak/providers/`
2. `kc.sh build`
3. `kc.sh start --optimized`
4. Optionally enable the `oidc-sweden-event-listener` event listener per realm for automatic setup of new realms.

## Java code style

The project uses the Spring code style (`instructions/spring-codestyle.xml`) and the inspections profile (`instructions/inspections.xml`). The rules below are derived from those files — follow them in all generated Java code.

### Formatting

- **Indentation**: 2 spaces (continuation indent: 4 spaces). No tabs.
- **Line length**: 120 characters maximum.
- **Blank lines**: at most 1 consecutive blank line anywhere.
- **`else`**, **`catch`**, and **`finally`** each start on a new line (not on the same line as the closing `}`).
- **`case`** labels are NOT indented relative to `switch`.
- When a binary expression wraps, place the operator at the start of the continuation line.
- When a ternary expression wraps, place `?` and `:` at the start of the continuation line.
- Wrap comments at the 120-character margin.
- Variable annotations (e.g. `@NonNull`) go on their own line above the declaration.
- Space before array initializer `{` and spaces inside array initializer `{ ... }`.

### Language idioms

- Declare all local variables and parameters `final` unless reassignment is required.
- Use pattern-matching `instanceof` (e.g. `if (obj instanceof Foo f)`) — never follow `instanceof` with an explicit cast.
- Always use braces around `if`, `for`, `while`, and `do` bodies — even single-statement bodies.
- Use `this.` when accessing instance fields and calling instance methods.
- Use `==` (not `.equals()`) when comparing enum constants.
- Add `serialVersionUID` to every class that implements `Serializable`.
- Annotate deprecated elements with both `@Deprecated` and a `@deprecated` Javadoc tag.

### Imports

- No wildcard imports (`import foo.*`). Always use explicit single-type imports.
- Prefer `https://` URLs over `http://` in code and documentation (plain `http://` triggers a warning).
