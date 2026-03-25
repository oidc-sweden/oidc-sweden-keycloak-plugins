# Instruction: Convert to multi-module structure

## Goal

Restructure the repository from a single-module project into a multi-module Maven project
that can host several plugins and utility JARs. Specifically:

1. Replace the current root `pom.xml` (which is a single-module JAR POM) with a parent
   POM (`packaging: pom`) that holds all shared configuration — versions, dependency
   management, plugin management, and developer/license metadata.
2. Create a `claims-plugin/` subdirectory and move all current source and resources into
   it, giving it its own `pom.xml` that inherits from the parent.
3. The `claims-plugin` module corresponds to the renamed `oidc-sweden-claims-plugin`
   artifact.

After this change the repository layout will be:

```
pom.xml                          ← parent POM (packaging: pom)
claims-plugin/
  pom.xml                        ← module POM (packaging: jar)
  src/
    main/
      java/
        se/oidc/keycloak/...     ← all existing Java sources (moved here)
      resources/
        META-INF/services/...   ← all existing service files (moved here)
    test/
      ...
  README.md                      ← moved here
instructions/
  ...                            ← unchanged
```

---

## Prerequisites — read before writing any code

Read the following files in full before making any changes:

1. `pom.xml` (current root POM — this becomes the basis for both the parent POM and the
   module POM)
2. `src/main/resources/META-INF/services/` — list all three service files to confirm
   their names
3. `README.md`

---

## Step 1 — Create the parent POM

Replace the **entire contents** of `pom.xml` with the following. Everything that is
specific to the `claims-plugin` artifact (dependencies, jar plugin config) moves into the
module POM in Step 2. The parent retains only what is shared across all future modules.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <groupId>se.oidc.keycloak</groupId>
  <artifactId>oidc-sweden-keycloak-plugins</artifactId>
  <version>1.0.0-SNAPSHOT</version>
  <packaging>pom</packaging>

  <n>OIDC Sweden :: Keycloak Plugins</n>
  <description>
    Parent POM for Keycloak plugins and utilities implementing the Swedish OIDC Claims and
    Scopes Specification 1.0
    (https://www.oidc.se/specifications/swedish-oidc-claims-specification-1_0.html).
  </description>
  <url>https://www.oidc.se</url>

  <licenses>
    <license>
      <n>The Apache Software License, Version 2.0</n>
      <url>https://www.apache.org/licenses/LICENSE-2.0.txt</url>
      <distribution>repo</distribution>
    </license>
  </licenses>

  <developers>
    <developer>
      <n>Martin Lindström</n>
      <email>martin@idsec.se</email>
      <organization>IDsec Solutions AB</organization>
      <organizationUrl>https://www.idsec.se</organizationUrl>
    </developer>
  </developers>

  <properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    <java.version>21</java.version>
    <keycloak.version>26.2.5</keycloak.version>
  </properties>

  <modules>
    <module>claims-plugin</module>
  </modules>

  <dependencyManagement>
    <dependencies>
      <dependency>
        <groupId>org.keycloak.bom</groupId>
        <artifactId>keycloak-spi-bom</artifactId>
        <version>${keycloak.version}</version>
        <type>pom</type>
        <scope>import</scope>
      </dependency>
      <!--
        Jackson: declared provided so modules do not bundle it. Keycloak already ships
        Jackson on its classpath. Pinned to the 2.17.x line that Keycloak 26.x uses.
        Update in step with Keycloak upgrades.
      -->
      <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.17.3</version>
      </dependency>
      <!--
        keycloak-server-spi-private and keycloak-services are not covered by
        keycloak-spi-bom so their versions must be pinned explicitly here.
      -->
      <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-server-spi-private</artifactId>
        <version>${keycloak.version}</version>
      </dependency>
      <dependency>
        <groupId>org.keycloak</groupId>
        <artifactId>keycloak-services</artifactId>
        <version>${keycloak.version}</version>
      </dependency>

      <!-- Test dependencies -->
      <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.13.4</version>
      </dependency>
      <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-core</artifactId>
        <version>5.23.0</version>
      </dependency>
      <dependency>
        <groupId>org.mockito</groupId>
        <artifactId>mockito-junit-jupiter</artifactId>
        <version>5.23.0</version>
      </dependency>
    </dependencies>
  </dependencyManagement>

  <build>
    <pluginManagement>
      <plugins>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-compiler-plugin</artifactId>
          <version>3.14.1</version>
          <configuration>
            <release>${java.version}</release>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-jar-plugin</artifactId>
          <version>3.4.2</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-clean-plugin</artifactId>
          <version>3.5.0</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-deploy-plugin</artifactId>
          <version>3.1.4</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-source-plugin</artifactId>
          <version>3.3.1</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-javadoc-plugin</artifactId>
          <version>3.11.3</version>
          <configuration>
            <doclint>all,-missing</doclint>
            <detectJavaApiLink>true</detectJavaApiLink>
          </configuration>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-surefire-plugin</artifactId>
          <version>3.5.4</version>
        </plugin>
        <plugin>
          <groupId>org.apache.maven.plugins</groupId>
          <artifactId>maven-enforcer-plugin</artifactId>
          <version>3.6.1</version>
          <executions>
            <execution>
              <id>enforce</id>
              <configuration>
                <rules>
                  <dependencyConvergence/>
                </rules>
              </configuration>
              <goals>
                <goal>enforce</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </pluginManagement>

    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-compiler-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-clean-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-enforcer-plugin</artifactId>
      </plugin>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-deploy-plugin</artifactId>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## Step 2 — Create `claims-plugin/pom.xml`

Create a new file at `claims-plugin/pom.xml` with the following contents. This module
POM inherits all shared configuration from the parent and declares only what is specific
to the claims plugin: its artifact identity, dependencies, and the jar manifest entries.

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
  <modelVersion>4.0.0</modelVersion>

  <parent>
    <groupId>se.oidc.keycloak</groupId>
    <artifactId>oidc-sweden-keycloak-plugins</artifactId>
    <version>1.0.0-SNAPSHOT</version>
  </parent>

  <artifactId>oidc-sweden-claims-plugin</artifactId>
  <packaging>jar</packaging>

  <n>OIDC Sweden :: Keycloak :: Claims Plugin</n>
  <description>
    Keycloak plugin providing OIDC Sweden protocol mappers, user profile attribute
    registration and client scope registration per the Swedish OIDC Claims and Scopes
    Specification 1.0 (https://www.oidc.se/specifications/swedish-oidc-claims-specification-1_0.html).
  </description>

  <dependencies>
    <dependency>
      <groupId>org.keycloak</groupId>
      <artifactId>keycloak-server-spi</artifactId>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.keycloak</groupId>
      <artifactId>keycloak-server-spi-private</artifactId>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.keycloak</groupId>
      <artifactId>keycloak-services</artifactId>
      <scope>provided</scope>
    </dependency>
    <dependency>
      <groupId>org.keycloak</groupId>
      <artifactId>keycloak-core</artifactId>
      <scope>provided</scope>
    </dependency>

    <dependency>
      <groupId>org.junit.jupiter</groupId>
      <artifactId>junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-core</artifactId>
      <scope>test</scope>
    </dependency>
    <dependency>
      <groupId>org.mockito</groupId>
      <artifactId>mockito-junit-jupiter</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-jar-plugin</artifactId>
        <configuration>
          <archive>
            <manifestEntries>
              <Implementation-Title>${project.artifactId}</Implementation-Title>
              <Implementation-Version>${project.version}</Implementation-Version>
            </manifestEntries>
          </archive>
        </configuration>
      </plugin>
    </plugins>
  </build>
</project>
```

---

## Step 3 — Move source and resources into `claims-plugin/`

Move the following directory trees from the repository root into `claims-plugin/`:

| From (repo root) | To |
|---|---|
| `src/main/java/` | `claims-plugin/src/main/java/` |
| `src/main/resources/` | `claims-plugin/src/main/resources/` |
| `src/test/` | `claims-plugin/src/test/` (if it exists) |
| `README.md` | `claims-plugin/README.md` |

After moving, the repository root must contain no `src/` directory and no `README.md`.
The root `pom.xml` is the only artifact remaining at the root (apart from `instructions/`
and any other repository-level files such as `.gitignore`).

Use plain filesystem move operations. If using the terminal:

```bash
cd /Users/martin/dev/oidc/oidc-sweden-keycloak-plugins
mkdir -p claims-plugin
mv src claims-plugin/src
mv README.md claims-plugin/README.md
```

---

## Step 4 — Update `claims-plugin/README.md` build section

After the move, the build command in `claims-plugin/README.md` must be updated to reflect
that the project is now built from the repository root.

Find the **`## Build`** section and replace it with:

```markdown
## Build

```bash
# Build all modules from the repository root:
mvn -DskipTests clean package

# Build only this module:
mvn -pl claims-plugin -am -DskipTests clean package
```
```

---

## Step 5 — Verification

After completing all steps, verify:

1. `pom.xml` at repository root has `<packaging>pom</packaging>` and a `<modules>` block
   containing `<module>claims-plugin</module>`.
2. `pom.xml` at repository root has no `<dependencies>` block (all dependencies are in
   `<dependencyManagement>` only).
3. `claims-plugin/pom.xml` exists, has a `<parent>` block referencing
   `oidc-sweden-keycloak-plugins`, and `<artifactId>` is `oidc-sweden-claims-plugin`.
4. `claims-plugin/pom.xml` declares no `<version>` on any dependency — all versions are
   inherited from the parent's `<dependencyManagement>`.
5. All three service files exist under
   `claims-plugin/src/main/resources/META-INF/services/`.
6. No `src/` directory exists at the repository root.
7. `claims-plugin/README.md` exists and its `## Build` section reflects the multi-module
   build commands.
8. `mvn clean package` run from the repository root completes successfully and produces
   `claims-plugin/target/oidc-sweden-claims-plugin-1.0.0-SNAPSHOT.jar`.
