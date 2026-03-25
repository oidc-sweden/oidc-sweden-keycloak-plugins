/*
 * Copyright 2026 OIDC Sweden
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
 * Defines the OIDC Sweden user profile attributes and attribute groups that are registered in Keycloak realms by the
 * OIDC Sweden plugin.
 *
 * <p>Attributes are organized into two named groups that appear as labeled sections in the
 * Keycloak Admin Console user profile editor:</p>
 * <ul>
 *   <li>{@value #GROUP_NATURAL_PERSON} — Swedish personal and coordination number attributes</li>
 *   <li>{@value #GROUP_ORG_ID} — Swedish organizational identity attributes</li>
 * </ul>
 *
 * <p>
 * The attributes {@code middleName} and {@code birthdate} are registered without a group.
 * </p>
 *
 * <p>
 * Authentication-event claims ({@code userSignature}, {@code credentialValidFrom},
 * {@code credentialValidTo}, {@code deviceIp}, {@code authnEvidence}, {@code authnProvider},
 * {@code userCertificate}) are not registered as user profile attributes — they are produced
 * by the authentication process and are not stored as persistent user properties.
 * </p>
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
   *
   * @return an unmodifiable list of all OIDC Sweden {@link UPAttribute} instances
   */
  public static List<UPAttribute> all() {
    return List.of(
        // --- no group ---
        attr("middleName", "Middle Name", null),
        attr("birthdate", "Date of Birth", null),

        // --- OIDC Sweden Natural Person group ---
        attr("personalIdentityNumber", "Personal Identity Number", GROUP_NATURAL_PERSON),
        attr("coordinationNumber", "Coordination Number", GROUP_NATURAL_PERSON),
        attr("coordinationNumberLevel", "Coordination Number Level", GROUP_NATURAL_PERSON),
        attr("previousCoordinationNumber", "Previous Coordination Number", GROUP_NATURAL_PERSON),

        // --- OIDC Sweden Organizational Identity group ---
        attr("orgAffiliation", "Organizational Affiliation", GROUP_ORG_ID),
        attr("orgName", "Organization Name", GROUP_ORG_ID),
        attr("orgNumber", "Organization Number", GROUP_ORG_ID),
        attr("orgUnit", "Organizational Unit", GROUP_ORG_ID)
    );
  }

  /**
   * Returns the names of all attributes returned by {@link #all()}.
   *
   * @return a list of attribute names in the same order as {@link #all()}
   */
  public static List<String> allNames() {
    return all().stream().map(UPAttribute::getName).toList();
  }

  /**
   * Returns the two OIDC Sweden attribute groups to register in the realm's user profile schema. These must be
   * registered before any attributes that reference them by name.
   *
   * @return a list of the two OIDC Sweden {@link UPGroup} instances
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
