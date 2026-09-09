/*
 * Copyright 2026 OIDC Sweden
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package se.oidc.keycloak.realm;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakSession;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import se.oidc.keycloak.claims.NaturalPersonInfoMapper;
import se.oidc.keycloak.claims.SwedishOidcClaimsMapper;
import se.oidc.keycloak.profile.SwedishOidcAttributes;
import se.oidc.keycloak.scopes.SwedishOidcScopes;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class OidcSwedenRealmResourceProviderTest {

  @Mock
  private KeycloakSession session;

  private Map<String, Object> infoEntity() {
    return OidcSwedenRealmResourceProvider.pluginInfo();
  }

  // --- getResource() ---

  @Test
  void getResource_returnsSelf() {
    final OidcSwedenRealmResourceProvider provider = new OidcSwedenRealmResourceProvider(this.session);
    assertSame(provider, provider.getResource());
  }

  // --- info(): plugin identification ---

  @Test
  void info_identifiesPluginAndSpecification() {
    final Map<String, Object> entity = this.infoEntity();
    assertEquals("oidc-sweden-claims-plugin", entity.get("plugin"));
    assertEquals("https://www.oidc.se/specifications/swedish-oidc-claims-specification-1_0.html",
        entity.get("specification"));
  }

  // --- info(): scopes ---

  @Test
  void info_listsAllSupportedScopes() {
    final List<String> expected = SwedishOidcScopes.all().stream()
        .map(SwedishOidcScopes.ScopeDefinition::name)
        .toList();
    assertEquals(expected, this.infoEntity().get("supportedScopes"));
  }

  // --- info(): attributes ---

  @Test
  void info_listsAllSupportedAttributes() {
    assertEquals(SwedishOidcAttributes.allNames(), this.infoEntity().get("supportedAttributes"));
  }

  // --- info(): protocol mappers ---

  @Test
  void info_listsBothProtocolMapperProviderIds() {
    assertEquals(
        List.of(SwedishOidcClaimsMapper.PROVIDER_ID, NaturalPersonInfoMapper.PROVIDER_ID),
        this.infoEntity().get("protocolMappers"));
  }

  // --- info(): no longer claims to manage anything ---

  @Test
  void info_doesNotClaimToManageScopesOrAttributes() {
    final Map<String, Object> entity = this.infoEntity();
    assertFalse(entity.containsKey("managedScopes"),
        "the plugin no longer manages scopes, so the field must be gone");
    assertFalse(entity.containsKey("managedAttributes"),
        "the plugin no longer manages attributes, so the field must be gone");
    assertTrue(entity.keySet().stream().noneMatch(key -> key.toLowerCase().contains("managed")),
        "no info field may describe anything as managed by the plugin: " + entity.keySet());
  }

  // --- info(): touches nothing in the realm ---

  @Test
  void constructingTheProviderDoesNotTouchTheSession() {
    new OidcSwedenRealmResourceProvider(this.session).getResource();
    Mockito.verifyNoInteractions(this.session);
  }
}
