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

import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

/**
 * Factory for {@link OidcSwedenEventListenerProvider}.
 */
public class OidcSwedenEventListenerProviderFactory implements EventListenerProviderFactory {

  /** The SPI provider ID used to register this event listener factory with Keycloak. */
  public static final String PROVIDER_ID = "oidc-sweden-event-listener";

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  /**
   * Creates a new {@link OidcSwedenEventListenerProvider} for the given session.
   *
   * @param session the active Keycloak session
   * @return a new provider instance
   */
  @Override
  public EventListenerProvider create(final KeycloakSession session) {
    return new OidcSwedenEventListenerProvider(session);
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
