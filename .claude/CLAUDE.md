# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this project is

A Keycloak 26.x plugin implementing the [Swedish OIDC Claims and Scopes Specification 1.0](https://www.oidc.se/specifications/swedish-oidc-claims-specification.html). It registers two protocol mappers and a realm resource provider into Keycloak via its SPI mechanism.

The plugin adds capability and does not reconfigure realms: it creates no client scope, no user profile attribute and no attribute group, in any realm, at startup or at any later point. Registering those is the operator's task, documented in the README and performed by `scripts/register-oidc-sweden.sh`. Do not reintroduce realm writes; `PluginWritesNothingTest` asserts their absence.

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

Output JAR: `claims-plugin/target/oidc-sweden-claims-plugin-*.jar`

## Architecture

The plugin is structured around Keycloak's SPI extension points. SPIs are registered in `src/main/resources/META-INF/services/`.

### Entry points

| Class | SPI | Description |
|-------|-----|-------------|
| `OidcSwedenRealmResourceProviderFactory` | `RealmResourceProviderFactory` | Exposes `GET /realms/{realm}/oidc-sweden/info`. `postInit()` is a no-op, there is no startup realm pass |
| `SwedishOidcClaimsMapper` | `ProtocolMapper` | Maps Swedish OIDC identity claims to tokens |
| `NaturalPersonInfoMapper` | `ProtocolMapper` | Maps `naturalPersonInfo` scope claims to standard OIDC profile claims |

### Info endpoint

`OidcSwedenRealmResourceProvider` serves the unauthenticated `GET /realms/{realm}/oidc-sweden/info`. It reports what the plugin *supports*, that is the scopes, attributes and mapper provider IDs read from the definition classes, not what any realm has registered. An answer confirms the JAR is deployed and loaded.

### Scopes, mappers, and attributes

- `SwedishOidcScopes` defines the three Swedish OIDC client scopes and which mapper to attach to each.
- `SwedishOidcAttributes` defines 10 persistent user profile attributes (identity, org, personal) and their two attribute groups.
- These two classes are definitions only, and nothing writes them into a realm. They are the single source feeding the info endpoint, the tests, the README and `scripts/register-oidc-sweden.sh`; change one and the others must follow.
- The two protocol mappers read these user attributes and Keycloak's built-in `firstName`/`lastName` fields to produce JWT claims.

## Key constants

- Scope URIs (e.g. `https://id.oidc.se/scope/naturalPersonNumber`) live in `SwedishOidcScopes`.
- Claim names and attribute keys live in `SwedishOidcAttributes`.
- Provider IDs: `oidc-sweden-claims-mapper` (`SwedishOidcClaimsMapper.PROVIDER_ID`), `natural-person-info-mapper` (`NaturalPersonInfoMapper.PROVIDER_ID`), `oidc-sweden` (`OidcSwedenRealmResourceProviderFactory.PROVIDER_ID`, the realm resource path).

## Dependencies

- **Keycloak 26.x** and **Jackson** are `provided` scope, bundled by Keycloak at runtime and not in the JAR.
- Tests use JUnit 5 and Mockito 5.

## Installation

1. Copy JAR to `/opt/keycloak/providers/`
2. `kc.sh build`
3. `kc.sh start --optimized`
4. Register the client scopes and user profile attributes per realm, with `scripts/register-oidc-sweden.sh` or by hand as described in the README. The plugin does not do this.

## Writing style

Never use em-dashes in any text you write: Markdown, Javadoc, code comments, commit
messages, log messages, string literals and chat answers alike. Rewrite the sentence
instead, with a comma, a colon, a semicolon, parentheses or two sentences. Where a
separator is genuinely wanted, such as between a key and its value in a listing, an em-dash may be used.

## Java code style

The project uses the Spring code style (`instructions/spring-codestyle.xml`) and the inspections profile (`instructions/inspections.xml`). The rules below are derived from those files. Follow them in all generated Java code.

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
- Use pattern-matching `instanceof` (e.g. `if (obj instanceof Foo f)`). Never follow `instanceof` with an explicit cast.
- Always use braces around `if`, `for`, `while`, and `do` bodies, even single-statement bodies.
- Use `this.` when accessing instance fields and calling instance methods.
- Use `==` (not `.equals()`) when comparing enum constants.
- Add `serialVersionUID` to every class that implements `Serializable`.
- Annotate deprecated elements with both `@Deprecated` and a `@deprecated` Javadoc tag.

### Imports

- No wildcard imports (`import foo.*`). Always use explicit single-type imports.
- Prefer `https://` URLs over `http://` in code and documentation (plain `http://` triggers a warning).
