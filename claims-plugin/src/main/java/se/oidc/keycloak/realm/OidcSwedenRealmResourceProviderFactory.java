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

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/**
 * Factory for {@link OidcSwedenRealmResourceProvider}.
 *
 * <p>The factory registers the read-only info endpoint and nothing else. It does not create, modify or remove any
 * client scope, user profile attribute or attribute group in any realm. Registering those is the operator's task,
 * see the {@code README} and {@code scripts/register-oidc-sweden.sh}.</p>
 */
public class OidcSwedenRealmResourceProviderFactory implements RealmResourceProviderFactory {

  /** The SPI provider ID used to register this factory with Keycloak. */
  public static final String PROVIDER_ID = "oidc-sweden";

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
  public void init(final Config.Scope config) {
  }

  @Override
  public void postInit(final KeycloakSessionFactory factory) {
  }

  @Override
  public void close() {
  }
}
