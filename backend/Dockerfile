# Utsav backend — Docker image for Render (free tier).
#
# Build:  maven:3.10.0-eclipse-temurin-25-noble  (verified tag on Docker Hub)
# Run:    eclipse-temurin:25-jre-noble           (verified tag on Docker Hub)
#
# The Docker build runs on Render with normal internet access, so:
#  - plain `mvn package` (online) is fine — no offline tricks needed here
#  - the `production` Maven profile runs vaadin:build-frontend, which downloads
#    Node.js and bundles the UI into the jar. No Node needed at runtime.

# ---- Build stage ----
FROM maven:3.10.0-eclipse-temurin-25-noble AS build
WORKDIR /app

# Copy the pom first so dependency downloads are cached in their own layer.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline -Pproduction || true

COPY src ./src
RUN mvn -B -Pproduction -DskipTests package

# ---- Run stage ----
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

COPY --from=build /app/target/utsav-backend-*.jar app.jar
COPY entrypoint.sh /app/entrypoint.sh
RUN chmod +x /app/entrypoint.sh

EXPOSE 8080
ENTRYPOINT ["/app/entrypoint.sh"]
