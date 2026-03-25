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
package se.oidc.keycloak.claims;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import se.oidc.keycloak.TestHelpers;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class NaturalPersonInfoMapperTest {

  private static final String SCOPE_NATURAL_PERSON_INFO = "https://id.oidc.se/scope/naturalPersonInfo";
  private static final String SCOPE_NATURAL_PERSON_NUMBER = "https://id.oidc.se/scope/naturalPersonNumber";

  private final NaturalPersonInfoMapper mapper = new NaturalPersonInfoMapper();

  @Mock
  private UserSessionModel userSession;

  @Mock
  private UserModel user;

  @Mock
  private KeycloakSession keycloakSession;

  @BeforeEach
  void setUp() {
    // Lenient: some tests have the mapper return early before these are ever called
    Mockito.lenient().when(this.userSession.getUser()).thenReturn(this.user);
    Mockito.lenient().when(this.user.getAttributeStream(Mockito.anyString())).thenAnswer(inv -> Stream.empty());
  }

  // --- Provider metadata ---

  @Test
  void getId_returnsCorrectProviderId() {
    assertEquals("natural-person-info-mapper", this.mapper.getId());
  }

  @Test
  void getDisplayType_returnsCorrectLabel() {
    assertEquals("OIDC Sweden — Natural Person Info", this.mapper.getDisplayType());
  }

  @Test
  void getDisplayCategory_returnsCorrectCategory() {
    assertEquals("Token mapper", this.mapper.getDisplayCategory());
  }

  @Test
  void getConfigProperties_returnsNonEmptyList() {
    assertNotNull(this.mapper.getConfigProperties());
    assertFalse(this.mapper.getConfigProperties().isEmpty());
  }

  // --- Scope active: all fields present ---

  @Test
  void setClaim_scopeActive_allFieldsPresent_allClaimsEmitted() {
    Mockito.when(this.user.getFirstName()).thenReturn("Anna");
    Mockito.when(this.user.getLastName()).thenReturn("Svensson");
    Mockito.when(this.user.getAttributeStream("middleName")).thenReturn(Stream.of("Maria"));
    Mockito.when(this.user.getAttributeStream("birthdate")).thenReturn(Stream.of("1990-06-15"));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_INFO));

    assertEquals("Anna", token.getOtherClaims().get("given_name"));
    assertEquals("Svensson", token.getOtherClaims().get("family_name"));
    assertEquals("Anna Svensson", token.getOtherClaims().get("name"));
    assertEquals("Maria", token.getOtherClaims().get("middle_name"));
    assertEquals("1990-06-15", token.getOtherClaims().get("birthdate"));
  }

  // --- Scope active: no middle name or birthdate ---

  @Test
  void setClaim_scopeActive_noMiddleNameOrBirthdate_onlyNameClaimsEmitted() {
    Mockito.when(this.user.getFirstName()).thenReturn("Anna");
    Mockito.when(this.user.getLastName()).thenReturn("Svensson");

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_INFO));

    assertNotNull(token.getOtherClaims().get("given_name"));
    assertNotNull(token.getOtherClaims().get("family_name"));
    assertNotNull(token.getOtherClaims().get("name"));
    assertNull(token.getOtherClaims().get("middle_name"));
    assertNull(token.getOtherClaims().get("birthdate"));
  }

  // --- Scope active: firstName only ---

  @Test
  void setClaim_scopeActive_firstNameOnlyNoLastName_givenNameAndNameEmitted() {
    Mockito.when(this.user.getFirstName()).thenReturn("Cher");
    Mockito.when(this.user.getLastName()).thenReturn(null);

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_INFO));

    assertEquals("Cher", token.getOtherClaims().get("given_name"));
    assertNull(token.getOtherClaims().get("family_name"));
    assertEquals("Cher", token.getOtherClaims().get("name"));
  }

  // --- Scope active: lastName only ---

  @Test
  void setClaim_scopeActive_lastNameOnlyNoFirstName_familyNameAndNameEmitted() {
    Mockito.when(this.user.getFirstName()).thenReturn(null);
    Mockito.when(this.user.getLastName()).thenReturn("Prince");

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_INFO));

    assertEquals("Prince", token.getOtherClaims().get("family_name"));
    assertNull(token.getOtherClaims().get("given_name"));
    assertEquals("Prince", token.getOtherClaims().get("name"));
  }

  // --- Scope active: both names null ---

  @Test
  void setClaim_scopeActive_bothNamesNull_noNameClaimsEmitted() {
    Mockito.when(this.user.getFirstName()).thenReturn(null);
    Mockito.when(this.user.getLastName()).thenReturn(null);

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_INFO));

    assertNull(token.getOtherClaims().get("given_name"));
    assertNull(token.getOtherClaims().get("family_name"));
    assertNull(token.getOtherClaims().get("name"));
  }

  // --- Scope not active ---

  @Test
  void setClaim_scopeNotActive_differentScopeRequested_noClaimsEmitted() {
    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_NUMBER));

    assertTrue(token.getOtherClaims().isEmpty());
  }

  // --- Blank firstName treated as absent ---

  @Test
  void setClaim_scopeActive_blankFirstName_givenNameAbsentFamilyNameEmitted() {
    Mockito.when(this.user.getFirstName()).thenReturn("  ");
    Mockito.when(this.user.getLastName()).thenReturn("Karlsson");

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_INFO));

    assertNull(token.getOtherClaims().get("given_name"));
    assertEquals("Karlsson", token.getOtherClaims().get("family_name"));
    assertEquals("Karlsson", token.getOtherClaims().get("name"));
  }

  // --- transformAccessToken ---

  @Test
  void transformAccessToken_scopeActiveAndIncludeInAccessTokenTrue_claimsPopulated() {
    Mockito.when(this.user.getFirstName()).thenReturn("Anna");
    Mockito.when(this.user.getLastName()).thenReturn("Svensson");

    final AccessToken token = new AccessToken();
    final AccessToken result = this.mapper.transformAccessToken(
        token, TestHelpers.mapperModel(false, true, false),
        this.keycloakSession, this.userSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_INFO));

    assertNotNull(result.getOtherClaims().get("given_name"));
    assertNotNull(result.getOtherClaims().get("family_name"));
  }

  @Test
  void transformAccessToken_includeInAccessTokenFalse_tokenReturnedUnchanged() {
    final AccessToken token = new AccessToken();
    final AccessToken result = this.mapper.transformAccessToken(
        token, TestHelpers.mapperModel(false, false, false),
        this.keycloakSession, this.userSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_INFO));

    assertTrue(result.getOtherClaims().isEmpty());
  }

  // --- transformUserInfoToken ---

  @Test
  void transformUserInfoToken_scopeActiveAndIncludeInUserInfoTrue_claimsPopulated() {
    Mockito.when(this.user.getFirstName()).thenReturn("Anna");
    Mockito.when(this.user.getLastName()).thenReturn("Svensson");

    final AccessToken token = new AccessToken();
    final AccessToken result = this.mapper.transformUserInfoToken(
        token, TestHelpers.mapperModel(false, false, true),
        this.keycloakSession, this.userSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_INFO));

    assertNotNull(result.getOtherClaims().get("given_name"));
  }

  // --- setClaim (ID token): includeInIDToken false ---

  @Test
  void setClaim_includeInIDTokenFalse_tokenNotPopulated() {
    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(false, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NATURAL_PERSON_INFO));

    assertTrue(token.getOtherClaims().isEmpty());
  }
}
