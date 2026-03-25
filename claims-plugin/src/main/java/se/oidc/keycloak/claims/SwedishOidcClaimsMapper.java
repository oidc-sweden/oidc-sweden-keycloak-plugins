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

import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link ProtocolMapper} that adds OIDC Sweden-defined OIDC claims based on requested OIDC Sweden-defined scopes.
 *
 * @author Martin Lindström
 */
public final class SwedishOidcClaimsMapper extends AbstractOIDCProtocolMapper
    implements ProtocolMapper, OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

  /** The SPI provider ID used to register this mapper with Keycloak. */
  public static final String PROVIDER_ID = "swedish-oidc-claims-mapper";

  private static final String SCOPE_NATURAL_PERSON_INFO =
      "https://id.oidc.se/scope/naturalPersonInfo";

  private static final String SCOPE_NATURAL_PERSON_NUMBER =
      "https://id.oidc.se/scope/naturalPersonNumber";

  private static final String SCOPE_NATURAL_PERSON_ORG_ID =
      "https://id.oidc.se/scope/naturalPersonOrgId";

  private static final String CLAIM_PERSONAL_ID_NUMBER =
      "https://id.oidc.se/claim/personalIdentityNumber";

  private static final String CLAIM_COORDINATION_NUMBER =
      "https://id.oidc.se/claim/coordinationNumber";

  private static final String CLAIM_COORDINATION_NUMBER_LEVEL =
      "https://id.oidc.se/claim/coordinationNumberLevel";

  private static final String CLAIM_PREVIOUS_COORDINATION_NUMBER =
      "https://id.oidc.se/claim/previousCoordinationNumber";

  private static final String CLAIM_ORG_NUMBER =
      "https://id.oidc.se/claim/orgNumber";

  private static final String CLAIM_ORG_AFFILIATION =
      "https://id.oidc.se/claim/orgAffiliation";

  private static final String CLAIM_ORG_NAME =
      "https://id.oidc.se/claim/orgName";

  private static final String CLAIM_ORG_UNIT =
      "https://id.oidc.se/claim/orgUnit";

  private static final String CLAIM_USER_CERTIFICATE =
      "https://id.oidc.se/claim/userCertificate";

  private static final String CLAIM_USER_SIGNATURE =
      "https://id.oidc.se/claim/userSignature";

  private static final String CLAIM_CREDENTIAL_VALID_FROM =
      "https://id.oidc.se/claim/credentialValidFrom";

  private static final String CLAIM_CREDENTIAL_VALID_TO =
      "https://id.oidc.se/claim/credentialValidTo";

  private static final String CLAIM_DEVICE_IP =
      "https://id.oidc.se/claim/deviceIp";

  private static final String CLAIM_AUTHN_EVIDENCE =
      "https://id.oidc.se/claim/authnEvidence";

  private static final String CLAIM_AUTHN_PROVIDER =
      "https://id.oidc.se/claim/authnProvider";

  private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

  static {
    final List<ProviderConfigProperty> props = new ArrayList<>();
    OIDCAttributeMapperHelper.addIncludeInTokensConfig(props, SwedishOidcClaimsMapper.class);
    CONFIG_PROPERTIES = Collections.unmodifiableList(props);
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public String getDisplayType() {
    return "OIDC Sweden";
  }

  @Override
  public String getDisplayCategory() {
    return "Token mapper";
  }

  @Override
  public String getHelpText() {
    return "Adds OIDC Sweden-defined claims based on requested scopes. "
        + "Claims are emitted as flat JSON keys under token 'otherClaims'.";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return CONFIG_PROPERTIES;
  }

  /**
   * Populates the ID token with OIDC Sweden claims for the active scopes when the mapper is configured to include
   * claims in the ID token.
   *
   * @param token the ID token being built
   * @param mappingModel the mapper configuration model
   * @param userSession the current user session
   * @param keycloakSession the active Keycloak session
   * @param clientSessionCtx the client session context, used to inspect the applied scopes
   */
  @Override
  protected void setClaim(
      final IDToken token,
      final org.keycloak.models.ProtocolMapperModel mappingModel,
      final UserSessionModel userSession,
      final KeycloakSession keycloakSession,
      final ClientSessionContext clientSessionCtx) {

    if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) {
      return;
    }
    this.applySwedishClaims(token, userSession, clientSessionCtx);
  }

  /**
   * Populates the access token with OIDC Sweden claims for the active scopes when the mapper is configured to
   * include claims in the access token.
   *
   * @param token the access token being built
   * @param mappingModel the mapper configuration model
   * @param keycloakSession the active Keycloak session
   * @param userSession the current user session
   * @param clientSessionCtx the client session context, used to inspect the applied scopes
   * @return the (possibly modified) access token
   */
  @Override
  public AccessToken transformAccessToken(
      final AccessToken token,
      final org.keycloak.models.ProtocolMapperModel mappingModel,
      final KeycloakSession keycloakSession,
      final UserSessionModel userSession,
      final ClientSessionContext clientSessionCtx) {

    if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) {
      return token;
    }
    this.applySwedishClaims(token, userSession, clientSessionCtx);
    return token;
  }

  /**
   * Populates the UserInfo response with OIDC Sweden claims for the active scopes when the mapper is configured
   * to include claims in the UserInfo endpoint.
   *
   * @param token the UserInfo token being built
   * @param mappingModel the mapper configuration model
   * @param keycloakSession the active Keycloak session
   * @param userSession the current user session
   * @param clientSessionCtx the client session context, used to inspect the applied scopes
   * @return the (possibly modified) UserInfo token
   */
  @Override
  public AccessToken transformUserInfoToken(
      final AccessToken token,
      final ProtocolMapperModel mappingModel,
      final KeycloakSession keycloakSession,
      final UserSessionModel userSession,
      final ClientSessionContext clientSessionCtx) {

    if (!OIDCAttributeMapperHelper.includeInUserInfo(mappingModel)) {
      return token;
    }

    this.applySwedishClaims(token, userSession, clientSessionCtx);
    return token;
  }

  private void applySwedishClaims(
      final IDToken token,
      final UserSessionModel userSession,
      final ClientSessionContext clientSessionCtx) {

    final Set<String> requestedScopes = this.getRequestedScopes(clientSessionCtx);
    final UserModel user = userSession.getUser();

    if (requestedScopes.contains(SCOPE_NATURAL_PERSON_INFO)) {
      // No-op by default.
    }

    if (requestedScopes.contains(SCOPE_NATURAL_PERSON_NUMBER)) {
      final String personalId = this.firstAttr(user, "personalIdentityNumber");
      final String coordination = this.firstAttr(user, "coordinationNumber");

      if (personalId != null) {
        token.getOtherClaims().put(CLAIM_PERSONAL_ID_NUMBER, personalId);
      }
      else if (coordination != null) {
        token.getOtherClaims().put(CLAIM_COORDINATION_NUMBER, coordination);

        final String level = this.firstAttr(user, "coordinationNumberLevel");
        if (level != null) {
          token.getOtherClaims().put(CLAIM_COORDINATION_NUMBER_LEVEL, level);
        }
      }

      final String previousCoordination = this.firstAttr(user, "previousCoordinationNumber");
      if (previousCoordination != null) {
        token.getOtherClaims().put(CLAIM_PREVIOUS_COORDINATION_NUMBER, previousCoordination);
      }
    }

    if (requestedScopes.contains(SCOPE_NATURAL_PERSON_ORG_ID)) {
      this.putIfPresent(token, user, "orgAffiliation", CLAIM_ORG_AFFILIATION);
      this.putIfPresent(token, user, "orgName", CLAIM_ORG_NAME);
      this.putIfPresent(token, user, "orgNumber", CLAIM_ORG_NUMBER);
      this.putIfPresent(token, user, "orgUnit", CLAIM_ORG_UNIT);
    }

    this.putIfPresent(token, user, "userCertificate", CLAIM_USER_CERTIFICATE);
    this.putIfPresent(token, user, "userSignature", CLAIM_USER_SIGNATURE);
    this.putIfPresentLong(token, user, "credentialValidFrom", CLAIM_CREDENTIAL_VALID_FROM);
    this.putIfPresentLong(token, user, "credentialValidTo", CLAIM_CREDENTIAL_VALID_TO);
    this.putIfPresent(token, user, "deviceIp", CLAIM_DEVICE_IP);
    this.putIfPresent(token, user, "authnEvidence", CLAIM_AUTHN_EVIDENCE);
    this.putIfPresent(token, user, "authnProvider", CLAIM_AUTHN_PROVIDER);
  }

  private Set<String> getRequestedScopes(final ClientSessionContext clientSessionCtx) {
    final Set<String> result = new HashSet<>();

    // Most reliable: look at *client scopes* that were actually applied to this session
    clientSessionCtx.getClientScopesStream().forEach(cs -> result.add(cs.getName()));

    // Keep the old fallback if you like, but the client scopes stream is the key.
    return result;
  }

  private String firstAttr(final UserModel user, final String attrName) {
    final List<String> values = user.getAttributeStream(attrName).toList();
    if (values.isEmpty()) {
      return null;
    }
    final String v = values.get(0);
    if (v == null || v.isBlank()) {
      return null;
    }
    return v;
  }

  private void putIfPresent(
      final IDToken token,
      final UserModel user,
      final String userAttr,
      final String claimName) {

    final String value = this.firstAttr(user, userAttr);
    if (value != null) {
      token.getOtherClaims().put(claimName, value);
    }
  }

  private void putIfPresentLong(
      final IDToken token,
      final UserModel user,
      final String userAttr,
      final String claimName) {

    final String value = this.firstAttr(user, userAttr);
    if (value == null) {
      return;
    }
    try {
      token.getOtherClaims().put(claimName, Long.parseLong(value));
    }
    catch (final NumberFormatException e) {
      // Ignore invalid numeric values.
    }
  }
}
