FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle settings.gradle /workspace/
COPY gradle /workspace/gradle
COPY src /workspace/src

RUN chmod +x /workspace/gradlew \
  && /workspace/gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

ARG GIT_SHA=unknown
LABEL org.opencontainers.image.revision="${GIT_SHA}" \
      org.opencontainers.image.source="https://github.com/Krezzy88/kien-shop-cart" \
      org.opencontainers.image.title="kien-shop-cart"

ENV TZ=UTC
ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=prod

RUN apt-get update \
  && apt-get install -y --no-install-recommends curl \
  && rm -rf /var/lib/apt/lists/*

RUN groupadd --system --gid 10001 appuser \
  && useradd --system --uid 10001 --gid appuser appuser
USER 10001

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -fsS http://127.0.0.1:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]