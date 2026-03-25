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
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import se.oidc.keycloak.TestHelpers;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SwedishOidcClaimsMapperTest {

  private static final String SCOPE_NUMBER = "https://id.oidc.se/scope/naturalPersonNumber";
  private static final String SCOPE_ORG_ID = "https://id.oidc.se/scope/naturalPersonOrgId";

  private static final String CLAIM_PERSONAL_ID = "https://id.oidc.se/claim/personalIdentityNumber";
  private static final String CLAIM_COORDINATION = "https://id.oidc.se/claim/coordinationNumber";
  private static final String CLAIM_COORDINATION_LEVEL = "https://id.oidc.se/claim/coordinationNumberLevel";
  private static final String CLAIM_PREV_COORDINATION = "https://id.oidc.se/claim/previousCoordinationNumber";
  private static final String CLAIM_ORG_AFFILIATION = "https://id.oidc.se/claim/orgAffiliation";
  private static final String CLAIM_ORG_NAME = "https://id.oidc.se/claim/orgName";
  private static final String CLAIM_ORG_NUMBER = "https://id.oidc.se/claim/orgNumber";
  private static final String CLAIM_ORG_UNIT = "https://id.oidc.se/claim/orgUnit";
  private static final String CLAIM_USER_CERTIFICATE = "https://id.oidc.se/claim/userCertificate";
  private static final String CLAIM_USER_SIGNATURE = "https://id.oidc.se/claim/userSignature";
  private static final String CLAIM_DEVICE_IP = "https://id.oidc.se/claim/deviceIp";
  private static final String CLAIM_AUTHN_EVIDENCE = "https://id.oidc.se/claim/authnEvidence";
  private static final String CLAIM_AUTHN_PROVIDER = "https://id.oidc.se/claim/authnProvider";
  private static final String CLAIM_CREDENTIAL_VALID_FROM = "https://id.oidc.se/claim/credentialValidFrom";
  private static final String CLAIM_CREDENTIAL_VALID_TO = "https://id.oidc.se/claim/credentialValidTo";

  private final SwedishOidcClaimsMapper mapper = new SwedishOidcClaimsMapper();

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
    assertEquals("swedish-oidc-claims-mapper", this.mapper.getId());
  }

  @Test
  void getDisplayType_returnsCorrectLabel() {
    assertEquals("OIDC Sweden", this.mapper.getDisplayType());
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

  // --- naturalPersonNumber scope: personalIdentityNumber ---

  @Test
  void setClaim_naturalPersonNumberScope_personalIdentityNumber_emitsPersonalIdClaim() {
    Mockito.when(this.user.getAttributeStream("personalIdentityNumber"))
        .thenReturn(Stream.of("196911292032"));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NUMBER));

    assertEquals("196911292032", token.getOtherClaims().get(CLAIM_PERSONAL_ID));
    assertNull(token.getOtherClaims().get(CLAIM_COORDINATION));
  }

  // --- naturalPersonNumber scope: coordinationNumber ---

  @Test
  void setClaim_naturalPersonNumberScope_coordinationNumber_emitsCoordinationClaims() {
    Mockito.when(this.user.getAttributeStream("coordinationNumber"))
        .thenReturn(Stream.of("198001011234"));
    Mockito.when(this.user.getAttributeStream("coordinationNumberLevel"))
        .thenReturn(Stream.of("confirmed"));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NUMBER));

    assertEquals("198001011234", token.getOtherClaims().get(CLAIM_COORDINATION));
    assertEquals("confirmed", token.getOtherClaims().get(CLAIM_COORDINATION_LEVEL));
    assertNull(token.getOtherClaims().get(CLAIM_PERSONAL_ID));
  }

  @Test
  void setClaim_naturalPersonNumberScope_coordinationNumberWithoutLevel_noLevelClaim() {
    Mockito.when(this.user.getAttributeStream("coordinationNumber"))
        .thenReturn(Stream.of("198001011234"));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NUMBER));

    assertNotNull(token.getOtherClaims().get(CLAIM_COORDINATION));
    assertNull(token.getOtherClaims().get(CLAIM_COORDINATION_LEVEL));
  }

  // --- naturalPersonNumber scope: previousCoordinationNumber ---

  @Test
  void setClaim_naturalPersonNumberScope_previousCoordinationNumberAlongsidePersonalId_bothClaimsEmitted() {
    Mockito.when(this.user.getAttributeStream("personalIdentityNumber"))
        .thenReturn(Stream.of("196911292032"));
    Mockito.when(this.user.getAttributeStream("previousCoordinationNumber"))
        .thenReturn(Stream.of("oldcoord123"));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NUMBER));

    assertNotNull(token.getOtherClaims().get(CLAIM_PERSONAL_ID));
    assertNotNull(token.getOtherClaims().get(CLAIM_PREV_COORDINATION));
  }

  // --- naturalPersonNumber scope: scope not requested ---

  @Test
  void setClaim_naturalPersonNumberScope_scopeNotRequested_noClaimsEmitted() {
    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext());

    assertTrue(token.getOtherClaims().isEmpty());
  }

  // --- naturalPersonOrgId scope: all org claims ---

  @Test
  void setClaim_naturalPersonOrgIdScope_allOrgAttributesSet_allClaimsEmitted() {
    Mockito.when(this.user.getAttributeStream("orgAffiliation"))
        .thenReturn(Stream.of("admin@5590026042"));
    Mockito.when(this.user.getAttributeStream("orgName"))
        .thenReturn(Stream.of("Litsec AB"));
    Mockito.when(this.user.getAttributeStream("orgNumber"))
        .thenReturn(Stream.of("5590026042"));
    Mockito.when(this.user.getAttributeStream("orgUnit"))
        .thenReturn(Stream.of("IT"));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_ORG_ID));

    assertEquals("admin@5590026042", token.getOtherClaims().get(CLAIM_ORG_AFFILIATION));
    assertEquals("Litsec AB", token.getOtherClaims().get(CLAIM_ORG_NAME));
    assertEquals("5590026042", token.getOtherClaims().get(CLAIM_ORG_NUMBER));
    assertEquals("IT", token.getOtherClaims().get(CLAIM_ORG_UNIT));
  }

  // --- naturalPersonOrgId scope: partial attributes ---

  @Test
  void setClaim_naturalPersonOrgIdScope_onlyOrgAffiliationSet_onlyThatClaimEmitted() {
    Mockito.when(this.user.getAttributeStream("orgAffiliation"))
        .thenReturn(Stream.of("admin@5590026042"));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_ORG_ID));

    assertNotNull(token.getOtherClaims().get(CLAIM_ORG_AFFILIATION));
    assertNull(token.getOtherClaims().get(CLAIM_ORG_NAME));
    assertNull(token.getOtherClaims().get(CLAIM_ORG_NUMBER));
    assertNull(token.getOtherClaims().get(CLAIM_ORG_UNIT));
  }

  // --- naturalPersonOrgId scope: scope not requested ---

  @Test
  void setClaim_naturalPersonOrgIdScope_differentScopeRequested_noOrgClaimsEmitted() {
    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NUMBER));

    assertNull(token.getOtherClaims().get(CLAIM_ORG_AFFILIATION));
    assertNull(token.getOtherClaims().get(CLAIM_ORG_NAME));
  }

  // --- Authentication-event claims: always emitted when attribute present ---

  @Test
  void setClaim_authnEventClaims_noScopeRequired_claimsAlwaysEmitted() {
    Mockito.when(this.user.getAttributeStream("userCertificate"))
        .thenReturn(Stream.of("base64cert"));
    Mockito.when(this.user.getAttributeStream("userSignature"))
        .thenReturn(Stream.of("base64sig"));
    Mockito.when(this.user.getAttributeStream("deviceIp"))
        .thenReturn(Stream.of("192.168.1.1"));
    Mockito.when(this.user.getAttributeStream("authnEvidence"))
        .thenReturn(Stream.of("evidence"));
    Mockito.when(this.user.getAttributeStream("authnProvider"))
        .thenReturn(Stream.of("https://provider.example.com"));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext());

    assertEquals("base64cert", token.getOtherClaims().get(CLAIM_USER_CERTIFICATE));
    assertEquals("base64sig", token.getOtherClaims().get(CLAIM_USER_SIGNATURE));
    assertEquals("192.168.1.1", token.getOtherClaims().get(CLAIM_DEVICE_IP));
    assertEquals("evidence", token.getOtherClaims().get(CLAIM_AUTHN_EVIDENCE));
    assertEquals("https://provider.example.com", token.getOtherClaims().get(CLAIM_AUTHN_PROVIDER));
  }

  // --- Authentication-event claims: numeric (credentialValidFrom / credentialValidTo) ---

  @Test
  void setClaim_credentialValidityAttributes_parsedAsLong() {
    Mockito.when(this.user.getAttributeStream("credentialValidFrom"))
        .thenReturn(Stream.of("1700000000"));
    Mockito.when(this.user.getAttributeStream("credentialValidTo"))
        .thenReturn(Stream.of("1800000000"));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext());

    assertEquals(1700000000L, token.getOtherClaims().get(CLAIM_CREDENTIAL_VALID_FROM));
    assertEquals(1800000000L, token.getOtherClaims().get(CLAIM_CREDENTIAL_VALID_TO));
  }

  // --- Authentication-event claims: invalid numeric value ignored ---

  @Test
  void setClaim_credentialValidFromInvalidNumber_claimAbsent() {
    Mockito.when(this.user.getAttributeStream("credentialValidFrom"))
        .thenReturn(Stream.of("not-a-number"));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext());

    assertNull(token.getOtherClaims().get(CLAIM_CREDENTIAL_VALID_FROM));
  }

  // --- Blank attribute value treated as absent ---

  @Test
  void setClaim_blankPersonalIdentityNumber_claimAbsent() {
    Mockito.when(this.user.getAttributeStream("personalIdentityNumber"))
        .thenReturn(Stream.of("   "));

    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(true, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NUMBER));

    assertNull(token.getOtherClaims().get(CLAIM_PERSONAL_ID));
  }

  // --- transformAccessToken ---

  @Test
  void transformAccessToken_scopeActiveAndIncludeInAccessTokenTrue_claimsPopulated() {
    Mockito.when(this.user.getAttributeStream("personalIdentityNumber"))
        .thenReturn(Stream.of("196911292032"));

    final AccessToken token = new AccessToken();
    final AccessToken result = this.mapper.transformAccessToken(
        token, TestHelpers.mapperModel(false, true, false),
        this.keycloakSession, this.userSession, TestHelpers.scopeContext(SCOPE_NUMBER));

    assertEquals("196911292032", result.getOtherClaims().get(CLAIM_PERSONAL_ID));
  }

  @Test
  void transformAccessToken_includeInAccessTokenFalse_tokenUnchanged() {
    final AccessToken token = new AccessToken();
    this.mapper.transformAccessToken(
        token, TestHelpers.mapperModel(false, false, false),
        this.keycloakSession, this.userSession, TestHelpers.scopeContext(SCOPE_NUMBER));

    assertTrue(token.getOtherClaims().isEmpty());
  }

  // --- transformUserInfoToken ---

  @Test
  void transformUserInfoToken_scopeActiveAndIncludeInUserInfoTrue_claimsPopulated() {
    Mockito.when(this.user.getAttributeStream("personalIdentityNumber"))
        .thenReturn(Stream.of("196911292032"));

    final AccessToken token = new AccessToken();
    final AccessToken result = this.mapper.transformUserInfoToken(
        token, TestHelpers.mapperModel(false, false, true),
        this.keycloakSession, this.userSession, TestHelpers.scopeContext(SCOPE_NUMBER));

    assertEquals("196911292032", result.getOtherClaims().get(CLAIM_PERSONAL_ID));
  }

  // --- setClaim (ID token): includeInIDToken false ---

  @Test
  void setClaim_includeInIDTokenFalse_tokenNotPopulated() {
    final IDToken token = new IDToken();
    this.mapper.setClaim(token, TestHelpers.mapperModel(false, false, false),
        this.userSession, this.keycloakSession, TestHelpers.scopeContext(SCOPE_NUMBER));

    assertTrue(token.getOtherClaims().isEmpty());
  }
}
