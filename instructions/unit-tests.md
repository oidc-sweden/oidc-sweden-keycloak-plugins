# Instruction: Write unit tests for `claims-plugin`

## Goal

Write unit tests for all testable logic in `claims-plugin`. The tests must use JUnit 5
and Mockito (already declared in the module POM). All test classes go under
`claims-plugin/src/test/java/`.

The Keycloak SPI interfaces (`UserModel`, `UserSessionModel`, `ClientSessionContext`,
`ClientScopeModel`, `ProtocolMapperModel`, `IDToken`, `AccessToken`) must be mocked with
Mockito — there is no embedded Keycloak available in unit tests.

The four classes under test are:

| Class | Package |
|---|---|
| `SwedishOidcClaimsMapper` | `se.oidc.keycloak.claims` |
| `NaturalPersonInfoMapper` | `se.oidc.keycloak.claims` |
| `SwedishOidcAttributes` | `se.oidc.keycloak.profile` |
| `SwedishOidcScopes` | `se.oidc.keycloak.scopes` |

---

## Prerequisites — read before writing any code

Read the following source files in full:

1. `claims-plugin/src/main/java/se/oidc/keycloak/claims/SwedishOidcClaimsMapper.java`
2. `claims-plugin/src/main/java/se/oidc/keycloak/claims/NaturalPersonInfoMapper.java`
3. `claims-plugin/src/main/java/se/oidc/keycloak/profile/SwedishOidcAttributes.java`
4. `claims-plugin/src/main/java/se/oidc/keycloak/scopes/SwedishOidcScopes.java`

---

## Mocking strategy

Because `IDToken` and `AccessToken` are concrete Keycloak classes (not interfaces), use
**real instances** of them rather than mocks — they carry an `otherClaims` map that the
mappers write into and tests can assert against directly.

Use Mockito mocks for: `UserModel`, `UserSessionModel`, `ClientSessionContext`,
`ClientScopeModel`, `ProtocolMapperModel`, `KeycloakSession`.

### Scope simulation helper

Both mappers read scopes from `clientSessionCtx.getClientScopesStream()`. Create a
package-private helper method (or inline it) that returns a mock
`ClientSessionContext` whose `getClientScopesStream()` returns a stream of mock
`ClientScopeModel` instances, each returning a given scope name from `getName()`.

Example pattern:

```java
private static ClientSessionContext scopeContext(final String... scopeNames) {
  final ClientSessionContext ctx = Mockito.mock(ClientSessionContext.class);
  final List<ClientScopeModel> scopes = Arrays.stream(scopeNames)
      .map(name -> {
        final ClientScopeModel s = Mockito.mock(ClientScopeModel.class);
        Mockito.when(s.getName()).thenReturn(name);
        return s;
      })
      .toList();
  Mockito.when(ctx.getClientScopesStream()).thenReturn(scopes.stream());
  return ctx;
}
```

### Mapper model helper

Both mappers check `OIDCAttributeMapperHelper.includeInIDToken(mappingModel)` etc. These
are static calls that read config map entries from the `ProtocolMapperModel`. Rather than
mocking them via PowerMock, use a **real `ProtocolMapperModel`** with its config map
populated with `"true"` for the relevant keys:

```java
private static ProtocolMapperModel mapperModel(
    final boolean idToken, final boolean accessToken, final boolean userInfo) {

  final ProtocolMapperModel model = new ProtocolMapperModel();
  final Map<String, String> config = new HashMap<>();
  config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN,     String.valueOf(idToken));
  config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, String.valueOf(accessToken));
  config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO,     String.valueOf(userInfo));
  model.setConfig(config);
  return model;
}
```

---

## Test class 1 — `SwedishOidcClaimsMapperTest`

File: `claims-plugin/src/test/java/se/oidc/keycloak/claims/SwedishOidcClaimsMapperTest.java`

### Provider metadata

- `getId()` returns `"swedish-oidc-claims-mapper"`
- `getDisplayType()` returns `"OIDC Sweden"`
- `getDisplayCategory()` returns `"Token mapper"`
- `getConfigProperties()` returns a non-null, non-empty list

### `naturalPersonNumber` scope — personalIdentityNumber

Set up a user with attribute `personalIdentityNumber` = `"196911292032"`. No
`coordinationNumber` attribute. Apply scope `naturalPersonNumber`.

Assert that the ID token contains:
- `https://id.oidc.se/claim/personalIdentityNumber` = `"196911292032"`
- `https://id.oidc.se/claim/coordinationNumber` is **absent**

### `naturalPersonNumber` scope — coordinationNumber (no personalIdentityNumber)

Set up a user with attribute `coordinationNumber` = `"198001011234"` and
`coordinationNumberLevel` = `"confirmed"`. No `personalIdentityNumber`.
Apply scope `naturalPersonNumber`.

Assert:
- `https://id.oidc.se/claim/coordinationNumber` = `"198001011234"`
- `https://id.oidc.se/claim/coordinationNumberLevel` = `"confirmed"`
- `https://id.oidc.se/claim/personalIdentityNumber` is **absent**

### `naturalPersonNumber` scope — coordinationNumber without level

Set up a user with `coordinationNumber` only (no level). Apply scope `naturalPersonNumber`.

Assert:
- `https://id.oidc.se/claim/coordinationNumber` is present
- `https://id.oidc.se/claim/coordinationNumberLevel` is **absent**

### `naturalPersonNumber` scope — previousCoordinationNumber emitted alongside personalIdentityNumber

Set up a user with both `personalIdentityNumber` and `previousCoordinationNumber`. Apply
scope `naturalPersonNumber`.

Assert both claims are present in the token.

### `naturalPersonNumber` scope — scope not requested

Set up a user with `personalIdentityNumber`. Apply **no** scopes (empty context).

Assert token `otherClaims` is empty.

### `naturalPersonOrgId` scope — all org claims present

Set up a user with attributes: `orgAffiliation` = `"admin@5590026042"`,
`orgName` = `"Litsec AB"`, `orgNumber` = `"5590026042"`, `orgUnit` = `"IT"`.
Apply scope `naturalPersonOrgId`.

Assert all four claims are present with correct values.

### `naturalPersonOrgId` scope — partial attributes

Set up a user with only `orgAffiliation` set. Apply scope `naturalPersonOrgId`.

Assert `orgAffiliation` claim is present, others are absent.

### `naturalPersonOrgId` scope — scope not requested

Apply a different scope (e.g. `naturalPersonNumber`) to a user with org attributes set.

Assert no org claims are emitted.

### Authentication-event claims — always emitted when attribute present

Set up a user with attributes: `userCertificate` = `"base64cert"`,
`userSignature` = `"base64sig"`, `deviceIp` = `"192.168.1.1"`,
`authnEvidence` = `"evidence"`, `authnProvider` = `"https://provider.example.com"`.
Apply **no** OIDC Sweden scopes (empty context).

Assert all five claims are present — these are emitted unconditionally regardless of scope.

### Authentication-event claims — numeric (credentialValidFrom / credentialValidTo)

Set up a user with `credentialValidFrom` = `"1700000000"` and
`credentialValidTo` = `"1800000000"`. Apply no scopes.

Assert:
- `https://id.oidc.se/claim/credentialValidFrom` = `Long` value `1700000000L`
- `https://id.oidc.se/claim/credentialValidTo` = `Long` value `1800000000L`

### Authentication-event claims — invalid numeric value ignored

Set up a user with `credentialValidFrom` = `"not-a-number"`. Apply no scopes.

Assert `https://id.oidc.se/claim/credentialValidFrom` is **absent** (invalid value silently
ignored).

### Blank attribute value treated as absent

Set up a user with `personalIdentityNumber` = `"   "` (whitespace only). Apply scope
`naturalPersonNumber`.

Assert `https://id.oidc.se/claim/personalIdentityNumber` is **absent**.

### `transformAccessToken` — scope active, includeInAccessToken true

Verify that `transformAccessToken` with a model that has `includeInAccessToken = true`
and scope `naturalPersonNumber` active populates the access token.

### `transformAccessToken` — includeInAccessToken false

Verify that `transformAccessToken` with `includeInAccessToken = false` does **not**
populate the token even when the scope is active.

### `transformUserInfoToken` — scope active, includeInUserInfo true

Verify that `transformUserInfoToken` with `includeInUserInfo = true` and scope
`naturalPersonNumber` active populates the token.

### `setClaim` (ID token) — includeInIDToken false

Verify that `setClaim` with `includeInIDToken = false` does **not** populate the token.

---

## Test class 2 — `NaturalPersonInfoMapperTest`

File: `claims-plugin/src/test/java/se/oidc/keycloak/claims/NaturalPersonInfoMapperTest.java`

### Provider metadata

- `getId()` returns `"natural-person-info-mapper"`
- `getDisplayType()` returns `"OIDC Sweden — Natural Person Info"`
- `getDisplayCategory()` returns `"Token mapper"`
- `getConfigProperties()` returns a non-null, non-empty list

### Scope active — all fields present

Set up a user with `firstName = "Anna"`, `lastName = "Svensson"`,
attribute `middleName = "Maria"`, attribute `birthdate = "1990-06-15"`.
Apply scope `naturalPersonInfo`.

Assert:
- `given_name` = `"Anna"`
- `family_name` = `"Svensson"`
- `name` = `"Anna Svensson"`
- `middle_name` = `"Maria"`
- `birthdate` = `"1990-06-15"`

### Scope active — no middle name or birthdate

Set up a user with first and last name only. Apply scope `naturalPersonInfo`.

Assert `given_name`, `family_name`, `name` present; `middle_name` and `birthdate` absent.

### Scope active — firstName only (no lastName)

`firstName = "Cher"`, `lastName = null`. Apply scope `naturalPersonInfo`.

Assert:
- `given_name` = `"Cher"`
- `family_name` absent
- `name` = `"Cher"`

### Scope active — lastName only (no firstName)

`firstName = null`, `lastName = "Prince"`. Apply scope `naturalPersonInfo`.

Assert:
- `family_name` = `"Prince"`
- `given_name` absent
- `name` = `"Prince"`

### Scope active — both names null

`firstName = null`, `lastName = null`. Apply scope `naturalPersonInfo`.

Assert `given_name`, `family_name`, and `name` are all absent.

### Scope not active — no claims emitted

Apply a different scope (e.g. `naturalPersonNumber`). User has all fields set.

Assert token `otherClaims` is empty.

### Blank firstName treated as absent

`firstName = "  "`, `lastName = "Karlsson"`. Apply scope `naturalPersonInfo`.

Assert `given_name` absent, `family_name` = `"Karlsson"`, `name` = `"Karlsson"`.

### `transformAccessToken` — scope active, includeInAccessToken true

Verify claims are populated in the access token.

### `transformAccessToken` — includeInAccessToken false

Verify token is returned unchanged.

### `transformUserInfoToken` — scope active, includeInUserInfo true

Verify claims are populated.

### `setClaim` (ID token) — includeInIDToken false

Verify token is not populated.

---

## Test class 3 — `SwedishOidcAttributesTest`

File: `claims-plugin/src/test/java/se/oidc/keycloak/profile/SwedishOidcAttributesTest.java`

No mocking needed — this class has no dependencies on Keycloak runtime.

### `all()` — correct count

Assert `SwedishOidcAttributes.all()` returns exactly 10 elements.

### `all()` — no null elements

Assert no element in the list is null.

### `all()` — expected attribute names present

Assert the list contains attributes with these exact names (order-independent):
`middleName`, `birthdate`, `personalIdentityNumber`, `coordinationNumber`,
`coordinationNumberLevel`, `previousCoordinationNumber`, `orgAffiliation`, `orgName`,
`orgNumber`, `orgUnit`.

### `all()` — group assignments correct

For each attribute assert the group assignment:

| Attribute | Expected group |
|---|---|
| `middleName` | `null` |
| `birthdate` | `null` |
| `personalIdentityNumber` | `"oidc-sweden-natural-person"` |
| `coordinationNumber` | `"oidc-sweden-natural-person"` |
| `coordinationNumberLevel` | `"oidc-sweden-natural-person"` |
| `previousCoordinationNumber` | `"oidc-sweden-natural-person"` |
| `orgAffiliation` | `"oidc-sweden-org-id"` |
| `orgName` | `"oidc-sweden-org-id"` |
| `orgNumber` | `"oidc-sweden-org-id"` |
| `orgUnit` | `"oidc-sweden-org-id"` |

### `all()` — permissions correct on all attributes

For every attribute assert:
- `getPermissions().getView()` contains `"admin"` and `"user"`
- `getPermissions().getEdit()` contains `"admin"` only (not `"user"`)

### `all()` — no attribute is multivalued

Assert `isMultivalued()` is false (or `getMultivalued()` is false/null) for every attribute.

### `allNames()` — matches names from `all()`

Assert `SwedishOidcAttributes.allNames()` equals the list of names extracted from
`all()` in the same order.

### `groups()` — correct count

Assert `SwedishOidcAttributes.groups()` returns exactly 2 elements.

### `groups()` — expected group names

Assert the two groups have names `"oidc-sweden-natural-person"` and `"oidc-sweden-org-id"`.

### `groups()` — display headers not blank

Assert `getDisplayHeader()` is non-null and non-blank for both groups.

### `groups()` — display descriptions not blank

Assert `getDisplayDescription()` is non-null and non-blank for both groups.

### Constants

Assert:
- `SwedishOidcAttributes.GROUP_NATURAL_PERSON` equals `"oidc-sweden-natural-person"`
- `SwedishOidcAttributes.GROUP_ORG_ID` equals `"oidc-sweden-org-id"`

---

## Test class 4 — `SwedishOidcScopesTest`

File: `claims-plugin/src/test/java/se/oidc/keycloak/scopes/SwedishOidcScopesTest.java`

No mocking needed.

### `all()` — correct count

Assert `SwedishOidcScopes.all()` returns exactly 3 elements.

### `all()` — expected scope names

Assert the three scope names are exactly:
- `"https://id.oidc.se/scope/naturalPersonInfo"`
- `"https://id.oidc.se/scope/naturalPersonNumber"`
- `"https://id.oidc.se/scope/naturalPersonOrgId"`

### `all()` — mapper assignments

Assert:
- `naturalPersonInfo` scope has `mapperProviderId` = `"natural-person-info-mapper"`
- `naturalPersonNumber` scope has `mapperProviderId` = `"swedish-oidc-claims-mapper"`
- `naturalPersonOrgId` scope has `mapperProviderId` = `"swedish-oidc-claims-mapper"`

### `all()` — descriptions not blank

Assert `description()` is non-null and non-blank for all three.

### `all()` — mapper names not blank

Assert `mapperName()` is non-null and non-blank for all three.

### Constants

Assert:
- `SwedishOidcScopes.NATURAL_PERSON_INFO` = `"https://id.oidc.se/scope/naturalPersonInfo"`
- `SwedishOidcScopes.NATURAL_PERSON_NUMBER` = `"https://id.oidc.se/scope/naturalPersonNumber"`
- `SwedishOidcScopes.NATURAL_PERSON_ORG_ID` = `"https://id.oidc.se/scope/naturalPersonOrgId"`

### `ScopeDefinition` record

Assert that `ScopeDefinition` exposes `name()`, `description()`, `mapperProviderId()`,
and `mapperName()` accessors and that a constructed instance returns the expected values.

---

## General requirements

- All test classes use `@ExtendWith(MockitoExtension.class)`.
- Test method names clearly describe the scenario and expected outcome, using a
  `given_when_then` or descriptive style.
- No test depends on another test's state.
- The `scopeContext()` and `mapperModel()` helpers may be placed in a shared
  package-private class `TestHelpers` in `se.oidc.keycloak` (under test sources) if
  used by more than one test class, or inlined as private static methods if used only once.
- After writing all tests, run `mvn -pl claims-plugin test` from the repository root and
  confirm all tests pass. Fix any compilation or test failures before finishing.
