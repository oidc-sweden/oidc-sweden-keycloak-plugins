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

A Keycloak plugin that implements the [Swedish OIDC Claims and Scopes Specification 1.0](https://www.oidc.se/specifications/swedish-oidc-claims-specification-1_0.html). It targets Keycloak 26.x and provides two protocol mappers that emit the OIDC Sweden claims.

The plugin adds capability and does not reconfigure realms. Deploying the JAR makes the two mapper types selectable in any realm; the three OIDC Sweden client scopes and the OIDC Sweden user profile attributes are registered by the operator, in the realms where they are wanted. See [Registering scopes and attributes](#cp-registration). The [example script](scripts/register-oidc-sweden.sh) does the whole job in one run.

### Artifact

**GroupID**: `se.oidc.keycloak`

**ArtifactID**: `oidc-sweden-claims-plugin`

<a name="cp-what-the-plugin-does"></a>
### What the plugin does

1. Registers two protocol mapper types (`SwedishOidcClaimsMapper` and `NaturalPersonInfoMapper`), selectable in any client or client scope in any realm.

2. Exposes a read-only [info endpoint](#cp-info-endpoint) reporting what the plugin supports, useful for confirming that the JAR is deployed and loaded.

That is all it does. It creates no client scope, no user profile attribute and no attribute group, in any realm, at startup or at any later point. Registering those is the operator's job, see [Registering scopes and attributes](#cp-registration).

> **Upgrading from 1.0.x:** earlier versions registered scopes and attributes in every realm at startup. Nothing is removed from realms that were configured that way, and they keep working unchanged. Realms set up from here on need the registration described below.

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

Requests a privacy-conscious subset of the standard `profile` scope, providing basic natural person information. This scope is a deliberate alternative to `profile`: it requests only the claims that are relevant for identifying a natural person without exposing all profile data.

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

Requests the Swedish personal or coordination identity number. Per the specification, `personalIdentityNumber` and `coordinationNumber` are mutually exclusive, since a person holds one or the other. Both are delivered in the ID token and UserInfo endpoint.

Note that `previousCoordinationNumber` and `coordinationNumberLevel` are not part of this scope and must be requested explicitly via the `claims` parameter.

| Claim | Notes |
|---|---|
| `personalIdentityNumber` | Essential. Emitted if present, XOR with coordinationNumber |
| `coordinationNumber` | Essential. Emitted if personalIdentityNumber is absent |

Mapper: `SwedishOidcClaimsMapper`

<a name="cp-naturalpersonorgid"></a>
#### naturalPersonOrgId (`https://id.oidc.se/scope/naturalPersonOrgId`)

Requests the organizational identity of a natural person affiliated with a Swedish organization.

| Claim | Token | Notes |
|---|---|---|
| `orgAffiliation` | ID token + UserInfo | Essential. Format `<personal-id>@<org-number>` |
| `name` | UserInfo | Full display name |
| `orgName` | UserInfo | Registered organization name |
| `orgNumber` | UserInfo | Swedish organizational number |

Mapper: `SwedishOidcClaimsMapper`

<a name="cp-user-profile-attributes"></a>
### User Profile Attributes

The following attributes belong in the realm's user profile schema, organised into two named groups that appear as labelled sections in the Keycloak Admin Console user profile editor. They are registered by the operator, not by the plugin, see [Registering scopes and attributes](#cp-registration).

All attributes have `view` permission for admin and user, `edit` permission for admin only, are not required, and are not multivalued.

**Ungrouped** (always visible):

| Attribute | Display name |
|---|---|
| `middleName` | Middle Name |
| `birthdate` | Date of Birth |

**Group `oidc-sweden-natural-person`, "OIDC Sweden: Natural Person"**

| Attribute | Display name |
|---|---|
| `personalIdentityNumber` | Personal Identity Number |
| `coordinationNumber` | Coordination Number |
| `coordinationNumberLevel` | Coordination Number Level |
| `previousCoordinationNumber` | Previous Coordination Number |

**Group `oidc-sweden-org-id`, "OIDC Sweden: Organisational Identity"**

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

**Display type**: `OIDC Sweden: Natural Person Info`

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

`kc.sh build` is required. Without it Keycloak does not pick up the new provider. Confirm the result with the [info endpoint](#cp-info-endpoint); the two mapper types are then selectable under **Client scopes → <scope> → Mappers → Configure a new mapper** in every realm.

Deploying the JAR changes no realm. Continue with [Registering scopes and attributes](#cp-registration) for each realm that should use the OIDC Sweden scopes.

<a name="cp-registration"></a>
### Registering scopes and attributes

Every realm that is to serve the OIDC Sweden scopes needs three client scopes and, unless the attributes come from somewhere else, the OIDC Sweden user profile attributes. This is a one-time setup per realm.

The quickest route is the example script in this repository, which does everything described in this section over the Admin REST API. It is idempotent, so anything already present is reported and left untouched:

```bash
./scripts/register-oidc-sweden.sh \
    --url https://kc.example.com --realm my-realm --user admin
```

It also takes `--path-prefix` (when Keycloak is served under a path, e.g. `/auth`), `--cacert` (to verify a TLS certificate issued by a private CA), `--admin-realm` and `--admin-client`; `--help` lists them all. It is meant to be read and adapted, and it fails with an explicit message if the mapper types are not deployed. It requires `curl` and `jq`.

The rest of this section is what the script does, for doing it by hand.

<a name="cp-registration-scopes"></a>
#### Client scopes

Create three client scopes. In the Admin Console: **Client scopes → Create client scope**, then for each scope open **Mappers → Configure a new mapper** and pick the mapper listed below.

| Setting | naturalPersonInfo | naturalPersonNumber | naturalPersonOrgId |
|---|---|---|---|
| **Name** | `https://id.oidc.se/scope/naturalPersonInfo` | `https://id.oidc.se/scope/naturalPersonNumber` | `https://id.oidc.se/scope/naturalPersonOrgId` |
| **Description** | Natural person information (given_name, family_name, middle_name, name, birthdate) | Swedish personal identity number or coordination number | Swedish organizational identity (orgAffiliation, orgName, orgNumber, orgUnit) |
| **Type** | None (assign per client) | None (assign per client) | None (assign per client) |
| **Protocol** | `openid-connect` | `openid-connect` | `openid-connect` |
| **Include in token scope** (`include.in.token.scope`) | `On` | `On` | `On` |
| **Display on consent screen** (`display.on.consent.screen`) | `On` | `On` | `On` |
| **Mapper** | `OIDC Sweden: Natural Person Info` (`natural-person-info-mapper`) | `OIDC Sweden` (`oidc-sweden-claims-mapper`) | `OIDC Sweden` (`oidc-sweden-claims-mapper`) |
| **Mapper name** | `natural-person-info-mapper` | `oidc-sweden-claims-mapper` | `oidc-sweden-claims-mapper` |

Every mapper is configured the same way, with all three inclusion switches on:

| Mapper setting | Config key | Value |
|---|---|---|
| Add to ID token | `id.token.claim` | `On` |
| Add to access token | `access.token.claim` | `On` |
| Add to userinfo | `userinfo.token.claim` | `On` |

If the mapper does not appear in the **Configure a new mapper** list, the JAR is not deployed or `kc.sh build` has not been run since it was added.

Over the Admin REST API, one request per scope creates the scope and its mapper together:

```http
POST /admin/realms/{realm}/client-scopes
Content-Type: application/json
Authorization: Bearer <admin token>

{
  "name": "https://id.oidc.se/scope/naturalPersonNumber",
  "description": "Swedish personal identity number or coordination number",
  "protocol": "openid-connect",
  "attributes": {
    "include.in.token.scope": "true",
    "display.on.consent.screen": "true"
  },
  "protocolMappers": [
    {
      "name": "oidc-sweden-claims-mapper",
      "protocol": "openid-connect",
      "protocolMapper": "oidc-sweden-claims-mapper",
      "config": {
        "id.token.claim": "true",
        "access.token.claim": "true",
        "userinfo.token.claim": "true"
      }
    }
  ]
}
```

Repeat with the name, description and mapper of each of the other two scopes. A `409 Conflict` means the scope is already there.

Finally, assign the scopes to the clients that may request them: **Clients → <client> → Client scopes → Add client scope**, added as **Optional** so that they apply when requested.

<a name="cp-registration-user-profile"></a>
#### User profile groups and attributes

Register the two attribute groups **before** the attributes. An attribute referencing a group that does not exist is rejected.

In the Admin Console the user profile is edited under **Realm settings → User profile**: the **Attribute groups** tab for the groups, the **Attributes** tab for the attributes. The **JSON editor** tab edits the same document directly, which is usually quicker for ten attributes.

**Attribute groups**

| Name | Display header | Display description |
|---|---|---|
| `oidc-sweden-natural-person` | OIDC Sweden: Natural Person | Swedish personal identity number and coordination number attributes per the Swedish OIDC Claims Specification. |
| `oidc-sweden-org-id` | OIDC Sweden: Organisational Identity | Swedish organisational identity attributes per the Swedish OIDC Claims Specification. |

**Attributes**

All ten are single-valued and not required, with **view** permission for `admin` and `user` and **edit** permission for `admin` only.

| Attribute | Display name | Group | View | Edit |
|---|---|---|---|---|
| `middleName` | Middle Name | *(none)* | admin, user | admin |
| `birthdate` | Date of Birth | *(none)* | admin, user | admin |
| `personalIdentityNumber` | Personal Identity Number | `oidc-sweden-natural-person` | admin, user | admin |
| `coordinationNumber` | Coordination Number | `oidc-sweden-natural-person` | admin, user | admin |
| `coordinationNumberLevel` | Coordination Number Level | `oidc-sweden-natural-person` | admin, user | admin |
| `previousCoordinationNumber` | Previous Coordination Number | `oidc-sweden-natural-person` | admin, user | admin |
| `orgAffiliation` | Organizational Affiliation | `oidc-sweden-org-id` | admin, user | admin |
| `orgName` | Organization Name | `oidc-sweden-org-id` | admin, user | admin |
| `orgNumber` | Organization Number | `oidc-sweden-org-id` | admin, user | admin |
| `orgUnit` | Organizational Unit | `oidc-sweden-org-id` | admin, user | admin |

Over the Admin REST API the user profile is one document: read it, add the groups and attributes to what is already there, and write it back. Dropping the existing entries would remove `username`, `email` and everything else the realm relies on.

```http
GET /admin/realms/{realm}/users/profile
Authorization: Bearer <admin token>
```

```http
PUT /admin/realms/{realm}/users/profile
Content-Type: application/json
Authorization: Bearer <admin token>

{
  "attributes": [
    ... the attributes already in the realm ...,
    {
      "name": "personalIdentityNumber",
      "displayName": "Personal Identity Number",
      "group": "oidc-sweden-natural-person",
      "multivalued": false,
      "permissions": { "view": ["admin", "user"], "edit": ["admin"] },
      "validations": {},
      "annotations": {}
    }
    ... and the other nine, "middleName" and "birthdate" without the "group" key ...
  ],
  "groups": [
    ... the groups already in the realm ...,
    {
      "name": "oidc-sweden-natural-person",
      "displayHeader": "OIDC Sweden: Natural Person",
      "displayDescription": "Swedish personal identity number and coordination number attributes per the Swedish OIDC Claims Specification.",
      "annotations": {}
    },
    {
      "name": "oidc-sweden-org-id",
      "displayHeader": "OIDC Sweden: Organisational Identity",
      "displayDescription": "Swedish organisational identity attributes per the Swedish OIDC Claims Specification.",
      "annotations": {}
    }
  ]
}
```

Skip any attribute the realm already defines. Its own definition is likely deliberate, and overwriting it may break a client that depends on it.

<a name="cp-info-endpoint"></a>
### Info endpoint

```
GET /realms/{realm}/oidc-sweden/info
```

Returns the scopes, user profile attributes and protocol mappers the plugin supports. None of them is created or managed by the plugin; the listing is a reference for whoever registers them:

```json
{
  "plugin": "oidc-sweden-claims-plugin",
  "specification": "https://www.oidc.se/specifications/swedish-oidc-claims-specification-1_0.html",
  "supportedScopes": [
    "https://id.oidc.se/scope/naturalPersonInfo",
    "https://id.oidc.se/scope/naturalPersonNumber",
    "https://id.oidc.se/scope/naturalPersonOrgId"
  ],
  "supportedAttributes": [
    "middleName", "birthdate", "personalIdentityNumber", "coordinationNumber",
    "coordinationNumberLevel", "previousCoordinationNumber", "orgAffiliation",
    "orgName", "orgNumber", "orgUnit"
  ],
  "protocolMappers": [
    "oidc-sweden-claims-mapper",
    "natural-person-info-mapper"
  ]
}
```

No authentication required. An answer means the JAR is deployed and loaded; it says nothing about what the realm has registered.


----

Copyright &copy; [The Swedish OpenID Connect Working Group (OIDC Sweden)](https://www.oidc.se), 2026. All Rights Reserved.
