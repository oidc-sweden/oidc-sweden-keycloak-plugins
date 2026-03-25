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
import se.oidc.keycloak.profile.SwedishOidcAttributes;
import se.oidc.keycloak.scopes.SwedishOidcScopes;

import java.util.Map;

/**
 * Realm resource provider exposing a read-only info endpoint at
 * {@code GET /realms/{realm}/oidc-sweden/info}.
 *
 * <p>Returns a JSON object describing the scopes and user profile attributes managed by
 * this plugin. Useful for operators to verify the plugin is loaded and active.</p>
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
   * Returns a JSON summary of the scopes and user profile attributes managed by the OIDC Sweden plugin
   * for the current realm.
   *
   * @return a {@code 200 OK} response containing a JSON object with {@code plugin}, {@code specification},
   *     {@code managedScopes}, and {@code managedAttributes} fields
   */
  @GET
  @Path("info")
  @Produces(MediaType.APPLICATION_JSON)
  public Response info() {
    final var scopes = SwedishOidcScopes.all().stream()
        .map(SwedishOidcScopes.ScopeDefinition::name)
        .toList();

    final var attributes = SwedishOidcAttributes.allNames();

    return Response.ok(Map.of(
        "plugin", "oidc-sweden-claims-plugin",
        "specification",
            "https://www.oidc.se/specifications/swedish-oidc-claims-specification-1_0.html",
        "managedScopes", scopes,
        "managedAttributes", attributes
    )).build();
  }

  @Override
  public void close() {
  }
}
