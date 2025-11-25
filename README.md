# todo-list — Build & Test

<!-- CI badge: replace OWNER/REPO with your GitHub org/repo -->
[![CI](https://github.com/sahlas/todo/actions/workflows/ci.yml/badge.svg)](https://github.com/sahlas/todo/actions/workflows/ci.yml)

This project uses Maven for building and testing. The repository includes Playwright as a test dependency and an `exec-maven-plugin` execution that runs Playwright's CLI to install browser binaries during the `pre-integration-test` phase.

Quick summary
- JDK required: Java 17 (project is configured for Java 17)
- Build tool: Maven 3.8+ (tested with Maven 3.9.10)

Preferred approach: Maven Toolchains (recommended)
-----------------------------------------------
To ensure forked test JVMs use the same JDK and receive the JVM flags needed for older libraries (e.g. Guice / cglib), use Maven Toolchains.

1. Create (or update) your `~/.m2/toolchains.xml` with the JDK 17 installation path you want Maven to use for forks. Example:

```text
<?xml version="1.0" encoding="UTF-8"?>
<!-- simplified example (no schemaLocation) to avoid editor lint warnings -->
<toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0">
  <toolchain>
    <type>jdk</type>
    <provides>
      <version>17</version>
      <vendor>oracle</vendor>
    </provides>
    <configuration>
      <jdkHome>/Users/YOUR_USER/Library/Java/JavaVirtualMachines/graalvm-jdk-17.0.12/Contents/Home</jdkHome>
    </configuration>
  </toolchain>
</toolchains>
```

Replace `/Users/YOUR_USER/Library/Java/JavaVirtualMachines/graalvm-jdk-17.0.12/Contents/Home` with the real path to your JDK 17.

Run the full build + tests
-------------------------
This will install Playwright browser binaries (via the exec plugin) and run unit/integration tests.

```bash
# from project root
mvn clean -DskipTests=false -X verify
```

Alternative (if you don't want toolchains)
------------------------------------------
You can instead export JVM flags that allow reflective access to core JDK packages and ensure the forked processes use the same Java binary by setting `JAVA_HOME` and `MAVEN_OPTS` in your shell.

```bash
# point to your JDK 17
export JAVA_HOME=/Users/<your-user>/Library/Java/JavaVirtualMachines/graalvm-jdk-17.0.12/Contents/Home
# add module opens required by older libraries (Guice/cglib)
export MAVEN_OPTS="--add-opens=java.base/java.lang=ALL-UNNAMED \
  --add-opens=java.base/java.lang.invoke=ALL-UNNAMED \
  --add-opens=java.base/java.lang.reflect=ALL-UNNAMED"

mvn clean -DskipTests=false -X verify
```

Notes about Playwright binaries
- The POM uses the exec plugin to run `com.microsoft.playwright.CLI install` during `pre-integration-test`. This should download and install the Playwright browser drivers into the usual Playwright cache location.
- If network interruptions are an issue, you can run the install step interactively first:

```bash
# run Playwright install using the project's test classpath
mvn -DskipTests=true org.codehaus.mojo:exec-maven-plugin:3.1.0:java -Dexec.mainClass=com.microsoft.playwright.CLI -Dexec.args="install"
```

Where test reports are produced
-----------------------------
- Unit test reports: `target/surefire-reports/`
- Integration test reports: `target/failsafe-reports/`
- Allure results (if used): `target/allure-results/`

Helpful tips / troubleshooting
-----------------------------
- If Guice / cglib fail with "InaccessibleObjectException" or NoClassDefFoundError during test initialization, ensure the forked JVM is using Java 17 and receives the `--add-opens` flags (toolchains or MAVEN_OPTS shown above).
- You may see an SLF4J NOP logger warning from Playwright; to enable logging in tests, add a test-scoped SLF4J binding, for example `org.slf4j:slf4j-simple` or `ch.qos.logback:logback-classic` in `pom.xml` under `<scope>test</scope>`.

Make the repo Maven-only (optional)
-----------------------------------
If you want the repo to be strictly Maven-only, you can remove Gradle/Gradle wrapper files (e.g. `build.gradle`, `gradlew`, `gradlew.bat`, `gradle/` folder). Only remove them if you won't need Gradle in future.

Contact / next steps
---------------------
If you want, I can:
- Add a test-scoped SLF4J logging binding to the POM to remove the NOP logger warning.
- Remove Gradle wrapper and Gradle files so the repo is Maven-only.
- Add a CI-friendly explanation for caching Playwright browsers.

Pick one and I will apply it and re-run the build/tests.

CI: caching Playwright browsers
------------------------------
To speed up CI runs and avoid re-downloading Playwright browser binaries every run, cache the Playwright browser artifacts and (optionally) the Maven local repository. The approach below sets a project-local Playwright browsers path and caches that directory between runs.

Why use a project-local browser path?
- Playwright downloads large browser binaries which can add many seconds/minutes to CI runs.
- Using a project-local path (instead of the default user cache) makes it easy to cache with typical CI cache actions.

Recommended environment variable
- Set `PLAYWRIGHT_BROWSERS_PATH` to a folder inside the repository (example: `.playwright-browsers`) so CI cache steps can store it.

GitHub Actions (example)
------------------------
Add a job step that caches the Maven repo and the Playwright browsers folder. Example workflow snippet:

```yaml
name: CI
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'

      - name: Cache Maven local repository
        uses: actions/cache@v4
        with:
          path: |
            ~/.m2/repository
          key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-m2-

      - name: Cache Playwright browsers
        uses: actions/cache@v4
        with:
          path: .playwright-browsers
          key: ${{ runner.os }}-playwright-browsers-${{ hashFiles('**/pom.xml') }}
          restore-keys: |
            ${{ runner.os }}-playwright-browsers-

      - name: Run Maven (install Playwright browsers + tests)
        env:
          # instruct Playwright to download browsers into a project folder we cache
          PLAYWRIGHT_BROWSERS_PATH: ${{ github.workspace }}/.playwright-browsers
        run: mvn clean -DskipTests=false verify
```

Notes for GitHub Actions
- The `PLAYWRIGHT_BROWSERS_PATH` env var forces Playwright to download browsers into `.playwright-browsers` inside the repo workspace so the `actions/cache` entry can store them between workflow runs.
- Caching the entire Maven repo (`~/.m2/repository`) speeds up dependency resolution. Use the `hashFiles('**/pom.xml')` key to bust the cache when dependencies change.

GitLab CI (example)
--------------------
A simple GitLab CI job that caches the Playwright browsers folder and Maven repo:

```yaml
stages:
  - build

variables:
  MAVEN_OPTS: "-Xmx2g"

cache:
  key: "${CI_COMMIT_REF_SLUG}"
  paths:
    - .playwright-browsers/
    - .m2/repository/

build:
  image: maven:3.9.10-eclipse-temurin-17
  stage: build
  script:
    - export PLAYWRIGHT_BROWSERS_PATH="$CI_PROJECT_DIR/.playwright-browsers"
    - mvn clean -DskipTests=false verify
```

Notes for GitLab CI
- GitLab caches are keyed by branch by default; you can configure a more global key to reuse caches across branches.
- Ensure the runner has enough disk space for the cached browsers.

Other CI systems
- The same concept applies: set `PLAYWRIGHT_BROWSERS_PATH` to a stable path inside the workspace and configure your CI caching mechanism to preserve that path across runs.

Troubleshooting
- If cached browsers are corrupted or a browser upgrade is needed, clear the cache (remove the `.playwright-browsers` cache entry) and re-run the pipeline to re-download fresh binaries.
- If your CI runner is ephemeral and you prefer not to cache, the Playwright install step will still run but may add time to the job.
