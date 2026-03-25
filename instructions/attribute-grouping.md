# Instruction: Add attribute groups and revise attribute set in `SwedishOidcAttributes`

## Goal

Modify `SwedishOidcAttributes.java` to:

1. Remove `userCertificate` from the registered attribute set entirely.
2. Assign `personalIdentityNumber`, `coordinationNumber`, `coordinationNumberLevel`, and
   `previousCoordinationNumber` to a new group named `oidc-sweden-natural-person`.
3. Assign `orgAffiliation`, `orgName`, `orgNumber`, and `orgUnit` to a new group named
   `oidc-sweden-org-id`.
4. Leave `middleName` and `birthdate` with no group (ungrouped).
5. Register the two groups in `OidcSwedenRealmSetup.ensureAttributes()` so that they are
   written to the realm's `UPConfig` alongside the attributes.
6. Update `README.md` to document the attribute grouping.

The groups will appear as collapsible sections in the Keycloak Admin Console user profile
editor, making it clear which attributes belong to the OIDC Sweden domain.

---

## Prerequisites — read before writing any code

Read the following files in full before making any changes:

1. `src/main/java/se/oidc/keycloak/profile/SwedishOidcAttributes.java`
2. `src/main/java/se/oidc/keycloak/realm/OidcSwedenRealmSetup.java`

---

## Step 1 — Rewrite `SwedishOidcAttributes.java`

Replace the entire contents of
`src/main/java/se/oidc/keycloak/profile/SwedishOidcAttributes.java` with the following:

```java
/*
 * Copyright 2025 OIDC Sweden
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.oidc.keycloak.profile;

import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPGroup;

import java.util.List;
import java.util.Set;

/**
 * Defines the OIDC Sweden user profile attributes and attribute groups that are registered
 * in Keycloak realms by the OIDC Sweden plugin.
 *
 * <p>Attributes are organised into two named groups that appear as labelled sections in the
 * Keycloak Admin Console user profile editor:</p>
 * <ul>
 *   <li>{@value #GROUP_NATURAL_PERSON} — Swedish personal and coordination number attributes</li>
 *   <li>{@value #GROUP_ORG_ID} — Swedish organisational identity attributes</li>
 * </ul>
 *
 * <p>The attributes {@code middleName} and {@code birthdate} are registered without a group.</p>
 *
 * <p>Authentication-event claims ({@code userSignature}, {@code credentialValidFrom},
 * {@code credentialValidTo}, {@code deviceIp}, {@code authnEvidence}, {@code authnProvider},
 * {@code userCertificate}) are not registered as user profile attributes — they are produced
 * by the authentication process and are not stored as persistent user properties.</p>
 */
public final class SwedishOidcAttributes {

  /** Group name for Swedish personal and coordination number attributes. */
  public static final String GROUP_NATURAL_PERSON = "oidc-sweden-natural-person";

  /** Group name for Swedish organisational identity attributes. */
  public static final String GROUP_ORG_ID = "oidc-sweden-org-id";

  private SwedishOidcAttributes() {
  }

  /**
   * Returns the full list of OIDC Sweden user profile attributes to register.
   */
  public static List<UPAttribute> all() {
    return List.of(
        // --- no group ---
        attr("middleName",                  "Middle Name",               null),
        attr("birthdate",                   "Date of Birth",             null),

        // --- OIDC Sweden Natural Person group ---
        attr("personalIdentityNumber",      "Personal Identity Number",  GROUP_NATURAL_PERSON),
        attr("coordinationNumber",          "Coordination Number",       GROUP_NATURAL_PERSON),
        attr("coordinationNumberLevel",     "Coordination Number Level", GROUP_NATURAL_PERSON),
        attr("previousCoordinationNumber",  "Previous Coordination Number", GROUP_NATURAL_PERSON),

        // --- OIDC Sweden Organizational Identity group ---
        attr("orgAffiliation",              "Organizational Affiliation", GROUP_ORG_ID),
        attr("orgName",                     "Organization Name",          GROUP_ORG_ID),
        attr("orgNumber",                   "Organization Number",        GROUP_ORG_ID),
        attr("orgUnit",                     "Organizational Unit",        GROUP_ORG_ID)
    );
  }

  /**
   * Returns the names of all attributes returned by {@link #all()}.
   */
  public static List<String> allNames() {
    return all().stream().map(UPAttribute::getName).toList();
  }

  /**
   * Returns the two OIDC Sweden attribute groups to register in the realm's user profile
   * schema. These must be registered before any attributes that reference them by name.
   */
  public static List<UPGroup> groups() {
    return List.of(
        group(GROUP_NATURAL_PERSON,
            "OIDC Sweden — Natural Person",
            "Swedish personal identity number and coordination number attributes "
                + "per the Swedish OIDC Claims Specification."),
        group(GROUP_ORG_ID,
            "OIDC Sweden — Organisational Identity",
            "Swedish organisational identity attributes "
                + "per the Swedish OIDC Claims Specification.")
    );
  }

  private static UPAttribute attr(
      final String name,
      final String displayName,
      final String group) {

    final UPAttribute attr = new UPAttribute();
    attr.setName(name);
    attr.setDisplayName(displayName);
    attr.setMultivalued(false);
    if (group != null) {
      attr.setGroup(group);
    }

    final UPAttributePermissions perms = new UPAttributePermissions();
    perms.setView(Set.of("admin", "user"));
    perms.setEdit(Set.of("admin"));
    attr.setPermissions(perms);

    return attr;
  }

  private static UPGroup group(
      final String name,
      final String displayHeader,
      final String displayDescription) {

    final UPGroup g = new UPGroup();
    g.setName(name);
    g.setDisplayHeader(displayHeader);
    g.setDisplayDescription(displayDescription);
    return g;
  }
}
```

---

## Step 2 — Update `OidcSwedenRealmSetup.ensureAttributes()`

The groups must be registered in `UPConfig` before the attributes that reference them, and
must be handled idempotently (only add missing groups, just as with attributes).

Open `src/main/java/se/oidc/keycloak/realm/OidcSwedenRealmSetup.java`.

Replace the entire `ensureAttributes` method with the following. The imports for
`UPGroup` and `java.util.List` must also be present — add them to the import block if not
already there.

```java
  /**
   * Ensures all OIDC Sweden user profile attributes and attribute groups are registered
   * in the given realm. Existing groups and attributes (matched by name) are left
   * untouched. Calls {@code UserProfileProvider.setConfiguration()} only if at least one
   * group or attribute was added.
   */
  static void ensureAttributes(final KeycloakSession session, final RealmModel realm) {
    session.getContext().setRealm(realm);
    final UserProfileProvider upp = session.getProvider(UserProfileProvider.class);
    final UPConfig config = upp.getConfiguration();

    boolean changed = false;

    // Ensure groups first — attributes reference groups by name, so groups must exist first
    for (final UPGroup group : SwedishOidcAttributes.groups()) {
      final String name = group.getName();
      final boolean exists = config.getGroups() != null && config.getGroups().stream()
          .anyMatch(g -> name.equals(g.getName()));
      if (!exists) {
        if (config.getGroups() == null) {
          config.setGroups(new java.util.ArrayList<>());
        }
        config.getGroups().add(group);
        changed = true;
        log.debugf("Realm '%s': registered user profile group '%s'", realm.getName(), name);
      }
    }

    // Ensure attributes
    for (final UPAttribute attribute : SwedishOidcAttributes.all()) {
      final String name = attribute.getName();
      final boolean exists = config.getAttributes().stream()
          .anyMatch(a -> name.equals(a.getName()));
      if (!exists) {
        config.getAttributes().add(attribute);
        changed = true;
        log.debugf("Realm '%s': registered user profile attribute '%s'",
            realm.getName(), name);
      }
    }

    if (changed) {
      upp.setConfiguration(config);
      log.infof("Realm '%s': OIDC Sweden user profile configuration updated", realm.getName());
    }
    else {
      log.debugf("Realm '%s': OIDC Sweden user profile configuration already up to date",
          realm.getName());
    }
  }
```

Required imports to add to `OidcSwedenRealmSetup.java` if not already present:

```java
import org.keycloak.representations.userprofile.config.UPGroup;
```

---

## Step 3 — Update `README.md`

In `README.md`, locate the **`## User Profile Attributes`** section and replace its
content to reflect the new grouping. The updated section must:

1. Explain that attributes are organised into two named groups that appear as labelled
   sections in the Keycloak Admin Console user profile editor.
2. Present the attributes in three subsections:

**Ungrouped** (no group, always visible):

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

3. State that all attributes have `view` permission for admin and user, `edit` permission
   for admin only, are not required, and are not multivalued.
4. Retain the existing explanation of why authentication-event claims are absent.

---

## Step 4 — Verification

After making the changes, verify:

1. `SwedishOidcAttributes.all()` returns exactly 10 attributes: `middleName` and
   `birthdate` (no group), plus 4 natural person attributes (group `oidc-sweden-natural-person`),
   plus 4 org-id attributes (group `oidc-sweden-org-id`). Neither `userCertificate` nor
   `phoneNumber` must appear.
2. `SwedishOidcAttributes.groups()` returns exactly 2 `UPGroup` instances with names
   `oidc-sweden-natural-person` and `oidc-sweden-org-id`.
3. `OidcSwedenRealmSetup.ensureAttributes()` registers groups before attributes.
4. The null-check on `config.getGroups()` is present before calling `.stream()` on it,
   since a freshly created realm may have a null groups list.
5. The `session.getContext().setRealm(realm)` call is still the first line of
   `ensureAttributes()` — it must not be removed.
6. No reference to `userCertificate` remains anywhere in `SwedishOidcAttributes.java`.
7. `README.md` `## User Profile Attributes` section reflects the grouping as described
   in Step 3.
