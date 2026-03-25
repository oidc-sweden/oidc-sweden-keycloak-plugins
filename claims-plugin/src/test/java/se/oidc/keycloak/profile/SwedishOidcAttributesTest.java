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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SwedishOidcAttributesTest {

  // --- all(): count ---

  @Test
  void all_returnsExactlyTenAttributes() {
    assertEquals(10, SwedishOidcAttributes.all().size());
  }

  // --- all(): no null elements ---

  @Test
  void all_noNullElements() {
    SwedishOidcAttributes.all().forEach(a -> assertNotNull(a, "null attribute found in all()"));
  }

  // --- all(): expected attribute names ---

  @Test
  void all_containsAllExpectedAttributeNames() {
    final List<String> names = SwedishOidcAttributes.allNames();
    assertTrue(names.contains("middleName"));
    assertTrue(names.contains("birthdate"));
    assertTrue(names.contains("personalIdentityNumber"));
    assertTrue(names.contains("coordinationNumber"));
    assertTrue(names.contains("coordinationNumberLevel"));
    assertTrue(names.contains("previousCoordinationNumber"));
    assertTrue(names.contains("orgAffiliation"));
    assertTrue(names.contains("orgName"));
    assertTrue(names.contains("orgNumber"));
    assertTrue(names.contains("orgUnit"));
  }

  // --- all(): group assignments ---

  @Test
  void all_groupAssignmentsCorrect() {
    final Map<String, String> groupByName = SwedishOidcAttributes.all().stream()
        .collect(Collectors.toMap(UPAttribute::getName, a -> a.getGroup() != null ? a.getGroup() : ""));

    assertEquals("", groupByName.get("middleName"));
    assertEquals("", groupByName.get("birthdate"));
    assertEquals("oidc-sweden-natural-person", groupByName.get("personalIdentityNumber"));
    assertEquals("oidc-sweden-natural-person", groupByName.get("coordinationNumber"));
    assertEquals("oidc-sweden-natural-person", groupByName.get("coordinationNumberLevel"));
    assertEquals("oidc-sweden-natural-person", groupByName.get("previousCoordinationNumber"));
    assertEquals("oidc-sweden-org-id", groupByName.get("orgAffiliation"));
    assertEquals("oidc-sweden-org-id", groupByName.get("orgName"));
    assertEquals("oidc-sweden-org-id", groupByName.get("orgNumber"));
    assertEquals("oidc-sweden-org-id", groupByName.get("orgUnit"));
  }

  // --- all(): permissions correct on all attributes ---

  @Test
  void all_permissionsCorrectOnAllAttributes() {
    for (final UPAttribute attr : SwedishOidcAttributes.all()) {
      assertNotNull(attr.getPermissions(), "permissions null for " + attr.getName());
      assertTrue(attr.getPermissions().getView().contains("admin"),
          "view missing 'admin' for " + attr.getName());
      assertTrue(attr.getPermissions().getView().contains("user"),
          "view missing 'user' for " + attr.getName());
      assertTrue(attr.getPermissions().getEdit().contains("admin"),
          "edit missing 'admin' for " + attr.getName());
      assertFalse(attr.getPermissions().getEdit().contains("user"),
          "edit should not contain 'user' for " + attr.getName());
    }
  }

  // --- all(): no attribute is multivalued ---

  @Test
  void all_noAttributeIsMultivalued() {
    for (final UPAttribute attr : SwedishOidcAttributes.all()) {
      assertFalse(attr.isMultivalued(), "attribute " + attr.getName() + " should not be multivalued");
    }
  }

  // --- allNames(): matches names from all() ---

  @Test
  void allNames_matchesNamesFromAll() {
    final List<String> expected = SwedishOidcAttributes.all().stream()
        .map(UPAttribute::getName)
        .toList();
    assertEquals(expected, SwedishOidcAttributes.allNames());
  }

  // --- groups(): count ---

  @Test
  void groups_returnsExactlyTwoGroups() {
    assertEquals(2, SwedishOidcAttributes.groups().size());
  }

  // --- groups(): expected group names ---

  @Test
  void groups_containsExpectedGroupNames() {
    final List<String> names = SwedishOidcAttributes.groups().stream()
        .map(UPGroup::getName)
        .toList();
    assertTrue(names.contains("oidc-sweden-natural-person"));
    assertTrue(names.contains("oidc-sweden-org-id"));
  }

  // --- groups(): display headers not blank ---

  @Test
  void groups_displayHeadersNotBlank() {
    for (final UPGroup group : SwedishOidcAttributes.groups()) {
      assertNotNull(group.getDisplayHeader(), "displayHeader null for " + group.getName());
      assertFalse(group.getDisplayHeader().isBlank(), "displayHeader blank for " + group.getName());
    }
  }

  // --- groups(): display descriptions not blank ---

  @Test
  void groups_displayDescriptionsNotBlank() {
    for (final UPGroup group : SwedishOidcAttributes.groups()) {
      assertNotNull(group.getDisplayDescription(), "displayDescription null for " + group.getName());
      assertFalse(group.getDisplayDescription().isBlank(),
          "displayDescription blank for " + group.getName());
    }
  }

  // --- Constants ---

  @Test
  void constants_correctValues() {
    assertEquals("oidc-sweden-natural-person", SwedishOidcAttributes.GROUP_NATURAL_PERSON);
    assertEquals("oidc-sweden-org-id", SwedishOidcAttributes.GROUP_ORG_ID);
  }
}
