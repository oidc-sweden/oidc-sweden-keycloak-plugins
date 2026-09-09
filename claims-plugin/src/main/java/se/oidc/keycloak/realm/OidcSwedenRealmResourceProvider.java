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
package se.oidc.keycloak.realm;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;
import se.oidc.keycloak.claims.NaturalPersonInfoMapper;
import se.oidc.keycloak.claims.SwedishOidcClaimsMapper;
import se.oidc.keycloak.profile.SwedishOidcAttributes;
import se.oidc.keycloak.scopes.SwedishOidcScopes;

import java.util.List;
import java.util.Map;

/**
 * Realm resource provider exposing a read-only info endpoint at
 * {@code GET /realms/{realm}/oidc-sweden/info}.
 *
 * <p>Returns a JSON object describing the scopes, user profile attributes and protocol mappers that this plugin
 * <em>supports</em>. The plugin does not create or manage any of them in a realm; the listing is a reference for
 * whoever registers them, and an answer from the endpoint confirms that the JAR is deployed and loaded.</p>
 */
public class OidcSwedenRealmResourceProvider implements RealmResourceProvider {

  private final KeycloakSession session;

  /**
   * Creates a new resource provider instance for the given session.
   *
   * @param session the active Keycloak session
   */
  public OidcSwedenRealmResourceProvider(final KeycloakSession session) {
    this.session = session;
  }

  /**
   * Returns this instance as the JAX-RS resource root.
   *
   * @return this provider instance
   */
  @Override
  public Object getResource() {
    return this;
  }

  /**
   * Returns a JSON summary of the scopes, user profile attributes and protocol mappers supported by the OIDC Sweden
   * plugin. Nothing in this listing is registered in the realm by the plugin itself.
   *
   * @return a {@code 200 OK} response carrying {@link #pluginInfo()}
   */
  @GET
  @Path("info")
  @Produces(MediaType.APPLICATION_JSON)
  public Response info() {
    return Response.ok(pluginInfo()).build();
  }

  /**
   * Builds the body of the info endpoint: what this plugin supports, taken from the definition classes so that the
   * endpoint cannot drift from them.
   *
   * @return a map with the {@code plugin}, {@code specification}, {@code supportedScopes},
   *     {@code supportedAttributes} and {@code protocolMappers} entries
   */
  static Map<String, Object> pluginInfo() {
    final List<String> scopes = SwedishOidcScopes.all().stream()
        .map(SwedishOidcScopes.ScopeDefinition::name)
        .toList();

    return Map.of(
        "plugin", "oidc-sweden-claims-plugin",
        "specification",
            "https://www.oidc.se/specifications/swedish-oidc-claims-specification-1_0.html",
        "supportedScopes", scopes,
        "supportedAttributes", SwedishOidcAttributes.allNames(),
        "protocolMappers", List.of(
            SwedishOidcClaimsMapper.PROVIDER_ID,
            NaturalPersonInfoMapper.PROVIDER_ID));
  }

  @Override
  public void close() {
  }
}
