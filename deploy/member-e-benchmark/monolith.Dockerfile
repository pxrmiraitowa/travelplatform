# Reuse the exact published product-service runtime; replace only its application JAR.
# This keeps the Linux/JRE runtime identical to the microservice being measured.
FROM ghcr.io/pxrmiraitowa/travel-platform-product-service@sha256:97a44668096f1da4092731807d08cda81030630b5377f03ae8de253b72f46c27
ARG SOURCE_REVISION
LABEL lab.travelplatform.source-revision=$SOURCE_REVISION \
      lab.travelplatform.variant="monolith" \
      org.opencontainers.image.title="Travel platform monolith benchmark"
COPY --chown=10001:10001 app.jar /app/app.jar
ENV SERVER_PORT=8080
EXPOSE 8080
