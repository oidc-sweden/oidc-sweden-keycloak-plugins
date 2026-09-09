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
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@ExtendWith(MockitoExtension.class)
class OidcSwedenRealmResourceProviderFactoryTest {

  @Mock
  private KeycloakSession session;

  @Mock
  private KeycloakSessionFactory sessionFactory;

  private final OidcSwedenRealmResourceProviderFactory factory =
      new OidcSwedenRealmResourceProviderFactory();

  // --- getId() ---

  @Test
  void getId_returnsProviderId() {
    assertEquals("oidc-sweden", this.factory.getId());
    assertEquals(OidcSwedenRealmResourceProviderFactory.PROVIDER_ID, this.factory.getId());
  }

  // --- create() ---

  @Test
  void create_returnsNewProviderPerCall() {
    final RealmResourceProvider first = this.factory.create(this.session);
    final RealmResourceProvider second = this.factory.create(this.session);

    assertInstanceOf(OidcSwedenRealmResourceProvider.class, first);
    assertNotSame(first, second);
  }

  @Test
  void create_doesNotTouchTheSession() {
    this.factory.create(this.session);
    Mockito.verifyNoInteractions(this.session);
  }

  // --- postInit(): no startup realm pass ---

  @Test
  void postInit_doesNothing() {
    this.factory.postInit(this.sessionFactory);

    // The startup pass over all realms is gone: the factory must not open a session, look up
    // realms, or write anything to any realm.
    Mockito.verifyNoInteractions(this.sessionFactory);
  }

  @Test
  void postInit_isSafeToCallRepeatedly() {
    this.factory.postInit(this.sessionFactory);
    this.factory.postInit(this.sessionFactory);
    this.factory.postInit(null);

    Mockito.verifyNoInteractions(this.sessionFactory);
  }

  // --- init() / close() ---

  @Test
  void initAndClose_doNothing() {
    this.factory.init(null);
    this.factory.close();
  }
}
