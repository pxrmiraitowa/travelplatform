FROM ghcr.io/pxrmiraitowa/travel-platform-product-service@sha256:97a44668096f1da4092731807d08cda81030630b5377f03ae8de253b72f46c27

ARG SOURCE_REVISION
ARG SERVICE_NAME
LABEL lab.travelplatform.source-revision=$SOURCE_REVISION \
      lab.travelplatform.variant="microservices" \
      lab.travelplatform.component=$SERVICE_NAME

COPY --chown=10001:10001 app.jar /app/app.jar
