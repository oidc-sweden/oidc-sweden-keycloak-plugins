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
package se.oidc.keycloak.scopes;

import se.oidc.keycloak.claims.NaturalPersonInfoMapper;
import se.oidc.keycloak.claims.SwedishOidcClaimsMapper;

import java.util.List;

/**
 * Defines the OIDC Sweden client scopes and their mapper associations.
 *
 * <p>Three scopes are defined per the Swedish OIDC Claims and Scopes Specification 1.0:</p>
 * <ul>
 *   <li>{@code https://id.oidc.se/scope/naturalPersonInfo}</li>
 *   <li>{@code https://id.oidc.se/scope/naturalPersonNumber}</li>
 *   <li>{@code https://id.oidc.se/scope/naturalPersonOrgId}</li>
 * </ul>
 */
public final class SwedishOidcScopes {

  /** URI for the {@code naturalPersonInfo} scope, as defined by the Swedish OIDC Claims Specification. */
  public static final String NATURAL_PERSON_INFO =
      "https://id.oidc.se/scope/naturalPersonInfo";

  /** URI for the {@code naturalPersonNumber} scope, as defined by the Swedish OIDC Claims Specification. */
  public static final String NATURAL_PERSON_NUMBER =
      "https://id.oidc.se/scope/naturalPersonNumber";

  /** URI for the {@code naturalPersonOrgId} scope, as defined by the Swedish OIDC Claims Specification. */
  public static final String NATURAL_PERSON_ORG_ID =
      "https://id.oidc.se/scope/naturalPersonOrgId";

  private SwedishOidcScopes() {
  }

  /**
   * Returns the full list of scope definitions to register.
   *
   * @return an unmodifiable list of all three {@link ScopeDefinition} instances
   */
  public static List<ScopeDefinition> all() {
    return List.of(
        new ScopeDefinition(
            NATURAL_PERSON_INFO,
            "Natural person information (given_name, family_name, middle_name, name, birthdate)",
            NaturalPersonInfoMapper.PROVIDER_ID,
            "natural-person-info-mapper"),

        new ScopeDefinition(
            NATURAL_PERSON_NUMBER,
            "Swedish personal identity number or coordination number",
            SwedishOidcClaimsMapper.PROVIDER_ID,
            "swedish-oidc-claims-mapper"),

        new ScopeDefinition(
            NATURAL_PERSON_ORG_ID,
            "Swedish organizational identity (orgAffiliation, orgName, orgNumber, orgUnit)",
            SwedishOidcClaimsMapper.PROVIDER_ID,
            "swedish-oidc-claims-mapper")
    );
  }

  /**
   * Describes a client scope and the mapper to attach to it.
   *
   * @param name             the scope name (URI)
   * @param description      human-readable description for the Admin Console
   * @param mapperProviderId the {@code ProtocolMapper} provider ID to attach
   * @param mapperName       the name to give the mapper instance on the scope
   */
  public record ScopeDefinition(
      String name,
      String description,
      String mapperProviderId,
      String mapperName) {
  }
}
