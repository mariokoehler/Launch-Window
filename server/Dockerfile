# Builds the Launch Window dedicated server image, deployed on the NAS via QNAP Container
# Station as a single-service Docker Compose "Application" (deploy/docker-compose.yml).
# Ported from the Robot-Rampage and StarWars projects' Dockerfiles, plus a data volume:
# unlike Robot-Rampage, this server keeps state (accounts, game-meta.json, players).
#
# Build context is the REPO ROOT, not server/ - the Maven reactor needs the root pom.xml and
# every module, and jgitver needs the real .git history to compute the version:
#
#   docker build -f server/Dockerfile -t launchwindow-server .

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
# The whole repo: Maven has to parse every module the root pom declares before -pl/-am apply.
# .dockerignore keeps target/, dev data and raw art out.
COPY . .
# -DskipTests: a version is only tagged after the full build and tests passed locally.
# APP_VERSION (plain "0.0.1", not "v0.0.1") overrides jgitver's auto-detection, which
# miscomputes a -SNAPSHOT version in GitHub Actions checkouts even exactly on a tag (seen in
# the StarWars project). Empty by default, so a local build falls back to jgitver's own.
ARG APP_VERSION=""
RUN if [ -n "$APP_VERSION" ]; then \
      mvn -pl server -am -DskipTests "-Djgitver.use-version=$APP_VERSION" package; \
    else \
      mvn -pl server -am -DskipTests package; \
    fi

# Not the -alpine variant: libGDX's native libraries are built against glibc, not musl.
FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --from=build /build/server/target/LaunchWindow-Server-*.jar /app/server.jar

# The game's data root: accounts.json, game-meta.json (the host edits it here: schedule,
# blackouts, declared goals, devMode), scheduler state, the frozen content pack and every
# player's snapshots, journal and reports. deploy/docker-compose.yml bind-mounts it to the NAS.
VOLUME /app/data

# TCP only (NetworkConstants.TCP_PORT). Documentation only; compose publishes it.
EXPOSE 45825/tcp

# ServerLauncher's arguments: the TCP port, then the data root.
ENTRYPOINT ["java", "--enable-native-access=ALL-UNNAMED", "-jar", "/app/server.jar", "45825", "/app/data"]
