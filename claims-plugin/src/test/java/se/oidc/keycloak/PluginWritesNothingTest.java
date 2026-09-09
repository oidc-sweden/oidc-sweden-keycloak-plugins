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
package se.oidc.keycloak;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Asserts that the plugin adds capability only: it registers the two protocol mapper types and the info endpoint, and
 * writes no realm configuration anywhere.
 *
 * <p>The removal of the startup realm pass and the realm-creation event listener is asserted here rather than
 * assumed: a reintroduced write would make these tests fail.</p>
 */
class PluginWritesNothingTest {

  private static final String PLUGIN_PACKAGE_PREFIX = "se.oidc.keycloak.";

  /**
   * Keycloak APIs that write realm or user profile configuration. None of them may appear anywhere in the plugin's
   * source, mapped to what a hit would mean.
   */
  private static final Map<String, String> FORBIDDEN_API_USAGE = Map.ofEntries(
      Map.entry("UserProfileProvider", "reads or writes the realm's user profile configuration"),
      Map.entry("UPConfig", "manipulates the realm's user profile configuration"),
      Map.entry("setConfiguration(", "writes the realm's user profile configuration"),
      Map.entry("addClientScope(", "creates a client scope in a realm"),
      Map.entry("removeClientScope(", "removes a client scope from a realm"),
      Map.entry("addDefaultClientScope(", "assigns a client scope in a realm"),
      Map.entry("addProtocolMapper(", "attaches a protocol mapper to a realm object"),
      Map.entry("runJobInTransaction", "opens a write transaction against the Keycloak store"),
      Map.entry("getRealmsStream", "walks the realms of the server"),
      Map.entry("EventListenerProvider", "hooks into realm lifecycle events"));

  private static Path mainSourceRoot() {
    // Surefire runs with the module directory as working directory; the second candidate covers a run
    // started from the repository root.
    for (final Path candidate : List.of(
        Path.of("src", "main", "java"),
        Path.of("claims-plugin", "src", "main", "java"))) {
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
    }
    throw new IllegalStateException("could not locate src/main/java from " + Path.of("").toAbsolutePath());
  }

  private static List<Path> mainSources() {
    try (Stream<Path> files = Files.walk(mainSourceRoot())) {
      return files.filter(p -> p.getFileName().toString().endsWith(".java")).toList();
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  /**
   * Returns the file's content with block and line comments blanked out, so that a mention of a forbidden API in
   * Javadoc is not mistaken for a call to it.
   */
  private static String codeWithoutComments(final Path file) {
    final String source;
    try {
      source = Files.readString(file, StandardCharsets.UTF_8);
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return source
        .replaceAll("(?s)/\\*.*?\\*/", "")
        .replaceAll("(?m)//.*$", "");
  }

  /**
   * Returns the classes this plugin registers under the given {@code META-INF/services} SPI, read from the test
   * classpath, that is, from what is packaged into the JAR. Registrations contributed by Keycloak's own JARs, which
   * sit on the same classpath, are filtered out by package.
   *
   * @param serviceName the fully qualified SPI interface name
   * @return the {@code se.oidc.*} implementation classes registered under it, in file order
   */
  private static List<String> registeredImplementations(final String serviceName) {
    final List<String> entries = new ArrayList<>();
    try {
      final Enumeration<URL> resources = PluginWritesNothingTest.class.getClassLoader()
          .getResources("META-INF/services/" + serviceName);
      while (resources.hasMoreElements()) {
        try (InputStream in = resources.nextElement().openStream()) {
          for (final String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
            final String trimmed = line.trim();
            if (trimmed.startsWith(PLUGIN_PACKAGE_PREFIX)) {
              entries.add(trimmed);
            }
          }
        }
      }
    }
    catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
    return entries;
  }

  // --- No code path writes realm or user profile configuration ---

  @Test
  void noSourceFileUsesAnApiThatWritesRealmConfiguration() {
    final List<String> hits = new ArrayList<>();
    for (final Path file : mainSources()) {
      final String code = codeWithoutComments(file);
      FORBIDDEN_API_USAGE.forEach((token, meaning) -> {
        if (code.contains(token)) {
          hits.add(file + " uses '" + token + "': " + meaning);
        }
      });
    }
    assertTrue(hits.isEmpty(),
        "the plugin must not write realm or user profile configuration, but:\n  " + String.join("\n  ", hits));
  }

  @Test
  void mainSourcesAreOnlyMappersDefinitionsAndTheInfoEndpoint() {
    final List<String> classNames = mainSources().stream()
        .map(p -> p.getFileName().toString())
        .sorted()
        .toList();

    assertEquals(List.of(
        "NaturalPersonInfoMapper.java",
        "OidcSwedenRealmResourceProvider.java",
        "OidcSwedenRealmResourceProviderFactory.java",
        "SwedishOidcAttributes.java",
        "SwedishOidcClaimsMapper.java",
        "SwedishOidcScopes.java"), classNames);
  }

  @Test
  void theRealmSetupClassIsGone() {
    assertFalse(Files.exists(mainSourceRoot().resolve(Path.of("se", "oidc", "keycloak", "realm",
        "OidcSwedenRealmSetup.java"))), "OidcSwedenRealmSetup must not be reintroduced");
    assertNull(PluginWritesNothingTest.class.getClassLoader()
            .getResource("se/oidc/keycloak/realm/OidcSwedenRealmSetup.class"),
        "OidcSwedenRealmSetup must not be packaged");
  }

  // --- The event listener SPI is gone ---

  @Test
  void theEventListenerPackageIsGone() {
    assertFalse(Files.exists(mainSourceRoot().resolve(Path.of("se", "oidc", "keycloak", "event"))),
        "the event listener package must not be reintroduced");
  }

  @Test
  void noEventListenerIsRegisteredWithTheEventListenerSpi() {
    // A realm whose event listener configuration still names the removed listener must not keep
    // Keycloak from starting; that holds precisely because the plugin registers no listener at all.
    assertEquals(List.of(),
        registeredImplementations("org.keycloak.events.EventListenerProviderFactory"),
        "the plugin must not register an EventListenerProviderFactory");
    assertFalse(Files.exists(Path.of("src", "main", "resources", "META-INF", "services",
            "org.keycloak.events.EventListenerProviderFactory")),
        "the event listener SPI registration file must not be reintroduced");
  }

  // --- What the plugin does register ---

  @Test
  void bothProtocolMapperTypesAreRegistered() {
    assertEquals(List.of(
        "se.oidc.keycloak.claims.SwedishOidcClaimsMapper",
        "se.oidc.keycloak.claims.NaturalPersonInfoMapper"),
        registeredImplementations("org.keycloak.protocol.ProtocolMapper"));
  }

  @Test
  void theInfoEndpointFactoryIsRegistered() {
    assertEquals(List.of("se.oidc.keycloak.realm.OidcSwedenRealmResourceProviderFactory"),
        registeredImplementations("org.keycloak.services.resource.RealmResourceProviderFactory"));
  }
}
