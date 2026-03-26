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
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Factory for {@link OidcSwedenRealmResourceProvider}.
 *
 * <p>On Keycloak startup ({@link #postInit}), iterates all realms and ensures that the
 * OIDC Sweden user profile attributes and client scopes are registered in each one.
 * Each operation is idempotent — existing attributes and scopes are never modified or
 * removed.</p>
 */
public class OidcSwedenRealmResourceProviderFactory implements RealmResourceProviderFactory {

  /** The SPI provider ID used to register this factory with Keycloak. */
  public static final String PROVIDER_ID = "oidc-sweden";

  private static final Logger log = Logger.getLogger(OidcSwedenRealmResourceProviderFactory.class);

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  /**
   * Creates a new {@link OidcSwedenRealmResourceProvider} for the given session.
   *
   * @param session the active Keycloak session
   * @return a new provider instance
   */
  @Override
  public RealmResourceProvider create(final KeycloakSession session) {
    return new OidcSwedenRealmResourceProvider(session);
  }

  @Override
  public void postInit(final KeycloakSessionFactory factory) {
    log.info("OIDC Sweden plugin: running startup realm setup");
    try {
      KeycloakModelUtils.runJobInTransaction(factory, session ->
          session.realms().getRealmsStream().forEach(realm -> {
            log.debugf("Processing realm '%s'", realm.getName());
            OidcSwedenRealmSetup.ensureAttributes(session, realm);
            OidcSwedenRealmSetup.ensureScopes(session, realm);
          })
      );
      log.info("OIDC Sweden plugin: startup realm setup complete");
    }
    catch (Exception e) {
      log.warnf("OIDC Sweden plugin: startup realm setup skipped — database not yet " +
          "available (%s). Existing realms will be configured on next restart.",
          e.getMessage());
    }
  }

  @Override
  public void init(final Config.Scope config) {
  }

  @Override
  public void close() {
  }
}
