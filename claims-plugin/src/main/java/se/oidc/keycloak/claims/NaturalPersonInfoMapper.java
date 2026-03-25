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
 * A {@link ProtocolMapper} that adds the standard OpenID Connect claims defined by the
 * {@code https://id.oidc.se/scope/naturalPersonInfo} scope: {@code given_name}, {@code family_name},
 * {@code middle_name}, {@code name}, {@code birthdate}.
 *
 * <p>
 * Claims are only emitted when {@code https://id.oidc.se/scope/naturalPersonInfo} is among the applied client scopes
 * for the session.
 * </p>
 *
 * @author Martin Lindström
 */
public final class NaturalPersonInfoMapper extends AbstractOIDCProtocolMapper
    implements ProtocolMapper, OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

  /** The SPI provider ID used to register this mapper with Keycloak. */
  public static final String PROVIDER_ID = "natural-person-info-mapper";

  private static final String SCOPE_NATURAL_PERSON_INFO =
      "https://id.oidc.se/scope/naturalPersonInfo";

  private static final List<ProviderConfigProperty> CONFIG_PROPERTIES;

  static {
    final List<ProviderConfigProperty> props = new ArrayList<>();
    OIDCAttributeMapperHelper.addIncludeInTokensConfig(props, NaturalPersonInfoMapper.class);
    CONFIG_PROPERTIES = Collections.unmodifiableList(props);
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public String getDisplayType() {
    return "OIDC Sweden — Natural Person Info";
  }

  @Override
  public String getDisplayCategory() {
    return "Token mapper";
  }

  @Override
  public String getHelpText() {
    return "Adds given_name, family_name, middle_name, name and birthdate claims "
        + "when the https://id.oidc.se/scope/naturalPersonInfo scope is applied.";
  }

  @Override
  public List<ProviderConfigProperty> getConfigProperties() {
    return CONFIG_PROPERTIES;
  }

  /**
   * Populates the ID token with natural-person claims when the
   * {@code naturalPersonInfo} scope is active and the mapper is configured to include claims in the ID token.
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
      final ProtocolMapperModel mappingModel,
      final UserSessionModel userSession,
      final KeycloakSession keycloakSession,
      final ClientSessionContext clientSessionCtx) {

    if (!OIDCAttributeMapperHelper.includeInIDToken(mappingModel)) {
      return;
    }
    this.applyNaturalPersonInfoClaims(token, userSession, clientSessionCtx);
  }

  /**
   * Populates the access token with natural-person claims when the {@code naturalPersonInfo} scope is active and
   * the mapper is configured to include claims in the access token.
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
      final ProtocolMapperModel mappingModel,
      final KeycloakSession keycloakSession,
      final UserSessionModel userSession,
      final ClientSessionContext clientSessionCtx) {

    if (!OIDCAttributeMapperHelper.includeInAccessToken(mappingModel)) {
      return token;
    }
    this.applyNaturalPersonInfoClaims(token, userSession, clientSessionCtx);
    return token;
  }

  /**
   * Populates the UserInfo response with natural-person claims when the {@code naturalPersonInfo} scope is active
   * and the mapper is configured to include claims in the UserInfo endpoint.
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
    this.applyNaturalPersonInfoClaims(token, userSession, clientSessionCtx);
    return token;
  }

  private void applyNaturalPersonInfoClaims(
      final IDToken token,
      final UserSessionModel userSession,
      final ClientSessionContext clientSessionCtx) {

    final Set<String> requestedScopes = new HashSet<>();
    clientSessionCtx.getClientScopesStream().forEach(cs -> requestedScopes.add(cs.getName()));

    if (!requestedScopes.contains(SCOPE_NATURAL_PERSON_INFO)) {
      return;
    }

    final UserModel user = userSession.getUser();

    final String firstName = user.getFirstName();
    final String lastName = user.getLastName();

    if (firstName != null && !firstName.isBlank()) {
      token.getOtherClaims().put("given_name", firstName);
    }
    if (lastName != null && !lastName.isBlank()) {
      token.getOtherClaims().put("family_name", lastName);
    }

    final String displayName = buildDisplayName(firstName, lastName);
    if (displayName != null) {
      token.getOtherClaims().put("name", displayName);
    }

    final String middleName = firstAttr(user, "middleName");
    if (middleName != null) {
      token.getOtherClaims().put("middle_name", middleName);
    }

    // birthdate format: YYYY-MM-DD per OpenID Connect Core
    final String birthdate = firstAttr(user, "birthdate");
    if (birthdate != null) {
      token.getOtherClaims().put("birthdate", birthdate);
    }
  }

  private static String buildDisplayName(final String firstName, final String lastName) {
    if ((firstName == null || firstName.isBlank()) && (lastName == null || lastName.isBlank())) {
      return null;
    }
    if (firstName == null || firstName.isBlank()) {
      return lastName;
    }
    if (lastName == null || lastName.isBlank()) {
      return firstName;
    }
    return firstName + " " + lastName;
  }

  private static String firstAttr(final UserModel user, final String attrName) {
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
}
