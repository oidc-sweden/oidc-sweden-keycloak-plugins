![Logo](https://www.oidc.se/img/oidc-logo.png)

# Keycloak Plugins


[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/se.oidc.keycloak/oidc-sweden-keycloak-plugins-parent.svg)](https://central.sonatype.com/artifact/se.oidc.keycloak/oidc-sweden-keycloak-plugins-parent)

Keycloak Plugin(s) for functionality defined by OIDC Sweden.

----

## About

This repository contain Keycloak plugin(s) that adds functionality defined in [Swedish OpenID Connect Specifications](https://www.oidc.se/specifications/).

## Distribution

The Keycloak plugin(s) are distributed via [Maven central](https://central.sonatype.com).

<a name="oidc-sweden-claims-plugin"></a>
## OIDC Sweden Claims Plugin

A Keycloak plugin that implements the [Swedish OIDC Claims and Scopes Specification 1.0](https://www.oidc.se/specifications/swedish-oidc-claims-specification-1_0.html). It targets Keycloak 26.x and provides two protocol mappers, automatic registration of OIDC Sweden user profile attributes, and automatic registration of the three OIDC Sweden client scopes in every realm. Deploying the JAR is sufficient — no manual Admin Console configuration is needed for scopes and attributes.

### Artifact

**GroupID**: `se.oidc.keycloak`

**ArtifactID**: `oidc-sweden-claims-plugin`

<a name="cp-what-the-plugin-does"></a>
### What the plugin does

1. Registers two protocol mapper types (`SwedishOidcClaimsMapper` and `NaturalPersonInfoMapper`) available in any client or client scope.

2. At startup, ensures all OIDC Sweden user profile attributes are present in every realm (idempotent — existing attributes are never modified or removed).

3. At startup, ensures all three OIDC Sweden client scopes are present in every realm, each with its mapper attached (idempotent — existing scopes are never modified or removed).

4. On new realm creation, repeats attribute and scope setup for that realm (requires event listener configuration — see [Installation](#cp-installation)).

<a name="cp-claims"></a>
### Claims

<a name="cp-user-identity-claims"></a>
#### User Identity Claims

| Claim URI | Keycloak attribute | Type | Description |
|---|---|---|---|
| `https://id.oidc.se/claim/`<br />`personalIdentityNumber` | `personalIdentityNumber` | String (12 digits) | Swedish civic registration number (personnummer) per [SKV 704](https://docs.swedenconnect.se/technical-framework/mirror/skv/skv704-8.pdf). |
| `https://id.oidc.se/claim/`<br />`coordinationNumber` | `coordinationNumber` | String (12 digits) | Swedish coordination number (samordningsnummer) per [SKV 707](https://docs.swedenconnect.se/technical-framework/mirror/skv/skv707-2.pdf). |
| `https://id.oidc.se/claim/`<br />`coordinationNumberLevel` | `coordinationNumberLevel` | String (`confirmed`, `probable`, `uncertain`) | Identity confirmation level for a coordination number. |
| `https://id.oidc.se/claim/`<br />`previousCoordinationNumber` | `previousCoordinationNumber` | String (12 digits) | Previously held coordination number, superseded by a personnummer. |

<a name="cp-organisational-identity-claims"></a>
#### Organisational Identity Claims

| Claim URI | Keycloak attribute | Type | Description |
|---|---|---|---|
| `https://id.oidc.se/claim/`<br />`orgNumber` | `orgNumber` | String (10 digits) | Swedish organizational number (organisationsnummer) per [SKV 709](https://docs.swedenconnect.se/technical-framework/mirror/skv/skv709-8.pdf). |
| `https://id.oidc.se/claim/`<br />`orgAffiliation` | `orgAffiliation` | String `<personal-id>@<org-number>` | Personal identity at a Swedish organization. |
| `https://id.oidc.se/claim/`<br />`orgName` | `orgName` | String | Registered organization name |
| `https://id.oidc.se/claim/`<br />`orgUnit` | `orgUnit` | String | Organizational unit name |

<a name="cp-authentication-information-claims"></a>
#### Authentication Information Claims

> **Note:** These claims are **not registered as user profile attributes** because they represent authentication-event data, not persistent user properties. They are emitted by `SwedishOidcClaimsMapper` if they happen to be present as user attributes from an external source (e.g. populated by an identity broker or SAML attribute mapper).

| Claim URI | Type | Description |
|---|---|---|
| `https://id.oidc.se/claim/userCertificate` | String (Base64) | X.509 certificate presented during authentication |
| `https://id.oidc.se/claim/userSignature` | String (Base64) | Signature produced during authentication |
| `https://id.oidc.se/claim/credentialValidFrom` | Integer (epoch seconds) | Start of credential validity period |
| `https://id.oidc.se/claim/credentialValidTo` | Integer (epoch seconds) | End of credential validity period |
| `https://id.oidc.se/claim/deviceIp` | String (IPv4/IPv6) | IP address of the device holding user credentials |
| `https://id.oidc.se/claim/authnEvidence` | String (Base64) | Proof or evidence about the authentication process |
| `https://id.oidc.se/claim/authnProvider` | String (URI preferred) | Identity of the authentication provider |

<a name="cp-scopes"></a>
### Scopes

<a name="cp-naturalpersoninfo"></a>
#### naturalPersonInfo (`https://id.oidc.se/scope/naturalPersonInfo`)

Requests a privacy-conscious subset of the standard `profile` scope, providing basic natural person information. This scope is a deliberate alternative to `profile` — it requests only the claims that are relevant for identifying a natural person without exposing all profile data.

| Claim | Type | Description |
|---|---|---|
| `given_name` | String | Given name(s) |
| `family_name` | String | Family name |
| `middle_name` | String | Middle name |
| `name` | String | Full display name (given + family) |
| `birthdate` | String (YYYY-MM-DD) | Date of birth per OpenID Connect Core |

Mapper: `NaturalPersonInfoMapper`

<a name="cp-naturalpersonnumber"></a>
#### naturalPersonNumber (`https://id.oidc.se/scope/naturalPersonNumber`)

Requests the Swedish personal or coordination identity number. Per the specification, `personalIdentityNumber` and `coordinationNumber` are mutually exclusive — a person holds one or the other. Both are delivered in the ID token and UserInfo endpoint.

Note that `previousCoordinationNumber` and `coordinationNumberLevel` are not part of this scope and must be requested explicitly via the `claims` parameter.

| Claim | Notes |
|---|---|
| `personalIdentityNumber` | Essential — emitted if present, XOR with coordinationNumber |
| `coordinationNumber` | Essential — emitted if personalIdentityNumber is absent |

Mapper: `SwedishOidcClaimsMapper`

<a name="cp-naturalpersonorgid"></a>
#### naturalPersonOrgId (`https://id.oidc.se/scope/naturalPersonOrgId`)

Requests the organizational identity of a natural person affiliated with a Swedish organization.

| Claim | Token | Notes |
|---|---|---|
| `orgAffiliation` | ID token + UserInfo | Essential — format `<personal-id>@<org-number>` |
| `name` | UserInfo | Full display name |
| `orgName` | UserInfo | Registered organization name |
| `orgNumber` | UserInfo | Swedish organizational number |

Mapper: `SwedishOidcClaimsMapper`

<a name="cp-user-profile-attributes"></a>
### User Profile Attributes

The plugin registers the following attributes in every realm's user profile schema, organised into two named groups that appear as labelled sections in the Keycloak Admin Console user profile editor. All attributes have `view` permission for admin and user, `edit` permission for admin only, are not required, and are not multivalued.

**Ungrouped** (always visible):

| Attribute | Display name |
|---|---|
| `middleName` | Middle Name |
| `birthdate` | Date of Birth |

**Group: `oidc-sweden-natural-person` — "OIDC Sweden — Natural Person"**

| Attribute | Display name |
|---|---|
| `personalIdentityNumber` | Personal Identity Number |
| `coordinationNumber` | Coordination Number |
| `coordinationNumberLevel` | Coordination Number Level |
| `previousCoordinationNumber` | Previous Coordination Number |

**Group: `oidc-sweden-org-id` — "OIDC Sweden — Organisational Identity"**

| Attribute | Display name |
|---|---|
| `orgAffiliation` | Organizational Affiliation |
| `orgName` | Organization Name |
| `orgNumber` | Organization Number |
| `orgUnit` | Organizational Unit |

Authentication-event claims (`userSignature`, `credentialValidFrom`, `credentialValidTo`, `deviceIp`, `authnEvidence`, `authnProvider`, `userCertificate`) are intentionally absent from this list. They represent transient authentication data produced during a login event and are not meaningful as stored user properties.

<a name="cp-mappers"></a>
### Mappers

<a name="cp-swedishoidcclaimsmapper"></a>
#### SwedishOidcClaimsMapper

**Provider ID**: `oidc-sweden-claims-mapper`

**Display type**: `OIDC Sweden`

Scope-driven mapper that fires on the `naturalPersonNumber` and `naturalPersonOrgId` scopes. Reads all Swedish OIDC user attributes from the Keycloak user model, including authentication-event claims if they happen to be present as user attributes. Emits them as flat JWT claims under `otherClaims`.

<a name="cp-naturalpersoninfomapper"></a>
#### NaturalPersonInfoMapper

**Provider ID**: `natural-person-info-mapper`

**Display type**: `OIDC Sweden — Natural Person Info`

Fires on the `naturalPersonInfo` scope. Maps `firstName` and `lastName` from Keycloak's built-in user fields, plus `middleName` and `birthdate` from user attributes, to the standard OpenID Connect claims `given_name`, `family_name`, `middle_name`, `name`, and `birthdate`.

### Build

```bash
# Build all modules from the repository root:
mvn -DskipTests clean package

# Build only this module:
mvn -pl claims-plugin -am -DskipTests clean package
```

<a name="cp-installation"></a>
### Installation

```bash
cp target/oidc-sweden-claims-plugin-<version>.jar /opt/keycloak/providers/
/opt/keycloak/bin/kc.sh build
/opt/keycloak/bin/kc.sh start --optimized
```

No Admin Console configuration is needed for scopes and attributes — they are registered automatically at startup for all existing realms.

For the event listener to fire on **new realm creation**, enable it in the master realm (and any other realm from which you create new realms):

**Realm settings → Events → Event listeners → add `oidc-sweden-event-listener`**

Or via the Admin REST API:

```http
PUT /admin/realms/{realm}
Content-Type: application/json

{ "eventsListeners": ["jboss-logging", "oidc-sweden-event-listener"] }
```

The startup `postInit()` pass runs unconditionally regardless of event listener configuration.

<a name="cp-info-endpoint"></a>
### Info endpoint

```
GET /realms/{realm}/oidc-sweden/info
```

Returns:

```json
{
  "plugin": "oidc-sweden-claims-plugin",
  "specification": "https://www.oidc.se/specifications/swedish-oidc-claims-specification-1_0.html",
  "managedScopes": [
    "https://id.oidc.se/scope/naturalPersonInfo",
    "https://id.oidc.se/scope/naturalPersonNumber",
    "https://id.oidc.se/scope/naturalPersonOrgId"
  ],
  "managedAttributes": [
    "middleName", "birthdate", "personalIdentityNumber", "coordinationNumber",
    "coordinationNumberLevel", "previousCoordinationNumber", "orgAffiliation",
    "orgName", "orgNumber", "orgUnit", "userCertificate"
  ]
}
```

No authentication required. Use to verify the plugin is loaded and active.


----

Copyright &copy; [The Swedish OpenID Connect Working Group (OIDC Sweden)](https://www.oidc.se), 2026. All Rights Reserved.
