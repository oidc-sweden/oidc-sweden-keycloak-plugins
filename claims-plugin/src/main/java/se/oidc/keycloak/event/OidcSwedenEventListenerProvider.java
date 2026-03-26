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
package se.oidc.keycloak.event;

import org.jboss.logging.Logger;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import se.oidc.keycloak.realm.OidcSwedenRealmSetup;

/**
 * Listens for new realm creation events and ensures that the OIDC Sweden user profile
 * attributes and client scopes are registered in newly created realms.
 *
 * <p>This complements the startup pass in
 * {@link se.oidc.keycloak.realm.OidcSwedenRealmResourceProviderFactory#postInit}, which
 * covers realms that exist when Keycloak starts. Together they ensure that all realms —
 * both existing and newly created — receive the OIDC Sweden setup.</p>
 *
 * <p>This listener must be enabled per-realm via Realm settings → Events → Event listeners
 * (add {@code oidc-sweden-event-listener}). The startup pass runs unconditionally regardless
 * of whether the event listener is configured.</p>
 */
public class OidcSwedenEventListenerProvider implements EventListenerProvider {

  private static final Logger log = Logger.getLogger(OidcSwedenEventListenerProvider.class);

  private final KeycloakSession session;

  /**
   * Creates a new provider instance for the given session.
   *
   * @param session the active Keycloak session
   */
  public OidcSwedenEventListenerProvider(final KeycloakSession session) {
    this.session = session;
  }

  /**
   * No-op — user-facing events are not relevant to this listener.
   *
   * @param event the user-facing event (ignored)
   */
  @Override
  public void onEvent(final Event event) {
    // Not interested in user-facing events
  }

  /**
   * Handles admin events; triggers OIDC Sweden realm setup when a new realm is created.
   *
   * @param adminEvent the admin event
   * @param includeRepresentation whether the event payload includes a resource representation
   */
  @Override
  public void onEvent(final AdminEvent adminEvent, final boolean includeRepresentation) {
    if (adminEvent.getOperationType() != OperationType.CREATE) {
      return;
    }
    if (adminEvent.getResourceType() != ResourceType.REALM) {
      return;
    }

    // The resource path for a realm CREATE event is the realm name
    final String realmName = adminEvent.getResourcePath();
    if (realmName == null || realmName.isBlank()) {
      log.warn("OIDC Sweden plugin: received REALM CREATE event with no resource path — skipping");
      return;
    }
    if ("master".equals(realmName)) {
      log.debugf("OIDC Sweden plugin: ignoring REALM CREATE event for master realm");
      return;
    }

    final RealmModel realm = session.realms().getRealmByName(realmName);
    if (realm == null) {
      log.warnf("OIDC Sweden plugin: realm '%s' not found after CREATE event — skipping",
          realmName);
      return;
    }

    log.infof("OIDC Sweden plugin: new realm '%s' detected, running setup", realmName);
    OidcSwedenRealmSetup.ensureAttributes(session, realm);
    OidcSwedenRealmSetup.ensureScopes(session, realm);
  }

  @Override
  public void close() {
  }
}
