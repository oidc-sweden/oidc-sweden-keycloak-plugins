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
package se.oidc.keycloak;

import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shared test helpers for mapper unit tests.
 */
public final class TestHelpers {

  private TestHelpers() {
  }

  /**
   * Returns a mock {@link ClientSessionContext} whose {@code getClientScopesStream()} yields the given scope names.
   */
  public static ClientSessionContext scopeContext(final String... scopeNames) {
    final ClientSessionContext ctx = Mockito.mock(ClientSessionContext.class);
    final List<ClientScopeModel> scopes = Arrays.stream(scopeNames)
        .map(name -> {
          final ClientScopeModel s = Mockito.mock(ClientScopeModel.class);
          // Lenient: mapper may return early before querying scope names
          Mockito.lenient().when(s.getName()).thenReturn(name);
          return s;
        })
        .toList();
    // Lenient: mapper may return early before calling getClientScopesStream()
    Mockito.lenient().when(ctx.getClientScopesStream()).thenReturn(scopes.stream());
    return ctx;
  }

  /**
   * Returns a real {@link ProtocolMapperModel} with include-in-token flags set as specified.
   */
  public static ProtocolMapperModel mapperModel(
      final boolean idToken, final boolean accessToken, final boolean userInfo) {

    final ProtocolMapperModel model = new ProtocolMapperModel();
    final Map<String, String> config = new HashMap<>();
    config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, String.valueOf(idToken));
    config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, String.valueOf(accessToken));
    config.put(OIDCAttributeMapperHelper.INCLUDE_IN_USERINFO, String.valueOf(userInfo));
    model.setConfig(config);
    return model;
  }
}
