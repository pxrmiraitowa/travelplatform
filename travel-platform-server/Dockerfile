FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /workspace

# Copy the dependency descriptor first so Docker can cache downloaded dependencies.
COPY pom.xml ./
RUN mvn -B dependency:go-offline

COPY src ./src

# CI runs tests before image packaging, so avoid running the same tests twice here.
RUN mvn -B clean package -DskipTests


FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl \
    && addgroup -S -g 10001 appgroup \
    && adduser -S -D -H -u 10001 -G appgroup appuser

WORKDIR /app

COPY --from=builder \
    --chown=appuser:appgroup \
    /workspace/target/travel-platform-server-*.jar \
    /app/app.jar

RUN mkdir -p /app/uploads \
    && chown -R appuser:appgroup /app/uploads

USER 10001:10001

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD curl --fail --silent http://127.0.0.1:8080/api/public/health > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
