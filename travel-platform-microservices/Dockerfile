FROM maven:3.9-eclipse-temurin-17 AS builder

ARG SERVICE_NAME
WORKDIR /workspace

COPY . .

RUN test -n "$SERVICE_NAME" \
    && mvn -B -pl "$SERVICE_NAME" -am clean package -DskipTests \
    && cp "$SERVICE_NAME"/target/*.jar /tmp/app.jar

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl \
    && addgroup -S -g 10001 appgroup \
    && adduser -S -D -H -u 10001 -G appgroup appuser

WORKDIR /app

COPY --from=builder --chown=appuser:appgroup /tmp/app.jar /app/app.jar

RUN mkdir -p /app/uploads \
    && chown -R appuser:appgroup /app/uploads

USER 10001:10001

EXPOSE 8000 8101 8102 8103 8104

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=8 \
    CMD curl --fail --silent http://127.0.0.1:${SERVER_PORT:-8000}/api/public/health > /dev/null || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
