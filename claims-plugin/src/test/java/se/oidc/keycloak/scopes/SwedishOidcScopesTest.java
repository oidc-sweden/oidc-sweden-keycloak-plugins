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
package se.oidc.keycloak.scopes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class SwedishOidcScopesTest {

  // --- all(): count ---

  @Test
  void all_returnsExactlyThreeScopes() {
    assertEquals(3, SwedishOidcScopes.all().size());
  }

  // --- all(): expected scope names ---

  @Test
  void all_containsExpectedScopeNames() {
    final List<String> names = SwedishOidcScopes.all().stream()
        .map(SwedishOidcScopes.ScopeDefinition::name)
        .toList();

    assertEquals("https://id.oidc.se/scope/naturalPersonInfo", names.get(0));
    assertEquals("https://id.oidc.se/scope/naturalPersonNumber", names.get(1));
    assertEquals("https://id.oidc.se/scope/naturalPersonOrgId", names.get(2));
  }

  // --- all(): mapper assignments ---

  @Test
  void all_mapperAssignmentsCorrect() {
    final SwedishOidcScopes.ScopeDefinition naturalPersonInfo = SwedishOidcScopes.all().get(0);
    final SwedishOidcScopes.ScopeDefinition naturalPersonNumber = SwedishOidcScopes.all().get(1);
    final SwedishOidcScopes.ScopeDefinition naturalPersonOrgId = SwedishOidcScopes.all().get(2);

    assertEquals("natural-person-info-mapper", naturalPersonInfo.mapperProviderId());
    assertEquals("swedish-oidc-claims-mapper", naturalPersonNumber.mapperProviderId());
    assertEquals("swedish-oidc-claims-mapper", naturalPersonOrgId.mapperProviderId());
  }

  // --- all(): descriptions not blank ---

  @Test
  void all_descriptionsNotBlank() {
    for (final SwedishOidcScopes.ScopeDefinition def : SwedishOidcScopes.all()) {
      assertNotNull(def.description(), "description null for " + def.name());
      assertFalse(def.description().isBlank(), "description blank for " + def.name());
    }
  }

  // --- all(): mapper names not blank ---

  @Test
  void all_mapperNamesNotBlank() {
    for (final SwedishOidcScopes.ScopeDefinition def : SwedishOidcScopes.all()) {
      assertNotNull(def.mapperName(), "mapperName null for " + def.name());
      assertFalse(def.mapperName().isBlank(), "mapperName blank for " + def.name());
    }
  }

  // --- Constants ---

  @Test
  void constants_correctValues() {
    assertEquals("https://id.oidc.se/scope/naturalPersonInfo", SwedishOidcScopes.NATURAL_PERSON_INFO);
    assertEquals("https://id.oidc.se/scope/naturalPersonNumber", SwedishOidcScopes.NATURAL_PERSON_NUMBER);
    assertEquals("https://id.oidc.se/scope/naturalPersonOrgId", SwedishOidcScopes.NATURAL_PERSON_ORG_ID);
  }

  // --- ScopeDefinition record ---

  @Test
  void scopeDefinitionRecord_accessorsReturnConstructorValues() {
    final SwedishOidcScopes.ScopeDefinition def = new SwedishOidcScopes.ScopeDefinition(
        "https://id.oidc.se/scope/test",
        "Test description",
        "test-provider-id",
        "test-mapper-name");

    assertEquals("https://id.oidc.se/scope/test", def.name());
    assertEquals("Test description", def.description());
    assertEquals("test-provider-id", def.mapperProviderId());
    assertEquals("test-mapper-name", def.mapperName());
  }
}
