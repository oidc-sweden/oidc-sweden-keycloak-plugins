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

import org.jboss.logging.Logger;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPGroup;
import org.keycloak.userprofile.UserProfileProvider;
import se.oidc.keycloak.profile.SwedishOidcAttributes;
import se.oidc.keycloak.scopes.SwedishOidcScopes;

import java.util.Map;

/**
 * Shared setup logic for ensuring OIDC Sweden user profile attributes and client scopes
 * are present in a Keycloak realm.
 *
 * <p>Called both at startup (via the factory's {@code postInit()} over all realms) and
 * on realm creation (via the event listener). All operations are idempotent.</p>
 */
public final class OidcSwedenRealmSetup {

  private static final Logger log = Logger.getLogger(OidcSwedenRealmSetup.class);

  private OidcSwedenRealmSetup() {
  }

  /**
   * Ensures all OIDC Sweden user profile attributes and attribute groups are registered
   * in the given realm. Existing groups and attributes (matched by name) are left
   * untouched. Calls {@code UserProfileProvider.setConfiguration()} only if at least one
   * group or attribute was added.
   *
   * @param session the active Keycloak session
   * @param realm the realm to configure
   */
  public static void ensureAttributes(final KeycloakSession session, final RealmModel realm) {
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

  /**
   * Ensures all three OIDC Sweden client scopes are registered in the given realm, each
   * with its mapper attached. Scopes already present (matched by name) are left untouched.
   *
   * @param session the active Keycloak session
   * @param realm the realm to configure
   */
  public static void ensureScopes(final KeycloakSession session, final RealmModel realm) {
    for (final SwedishOidcScopes.ScopeDefinition def : SwedishOidcScopes.all()) {
      final boolean exists = realm.getClientScopesStream()
          .anyMatch(s -> def.name().equals(s.getName()));

      if (exists) {
        log.debugf("Realm '%s': client scope '%s' already present",
            realm.getName(), def.name());
        continue;
      }

      final ClientScopeModel scope = realm.addClientScope(def.name());
      scope.setProtocol("openid-connect");
      scope.setDescription(def.description());
      scope.setAttribute("include.in.token.scope", "true");
      scope.setAttribute("display.on.consent.screen", "true");

      final ProtocolMapperModel mapper = new ProtocolMapperModel();
      mapper.setName(def.mapperName());
      mapper.setProtocolMapper(def.mapperProviderId());
      mapper.setProtocol("openid-connect");
      mapper.setConfig(Map.of(
          OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true",
          OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN,     "true",
          OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO,     "true"
      ));
      scope.addProtocolMapper(mapper);

      log.infof("Realm '%s': registered client scope '%s' with mapper '%s'",
          realm.getName(), def.name(), def.mapperName());
    }
  }
}
