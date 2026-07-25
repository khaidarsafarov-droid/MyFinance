FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN sh ./gradlew --configure-on-demand -Dorg.gradle.java.home="$JAVA_HOME" \
    :backend:server:installDist --no-daemon

FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --create-home app
WORKDIR /app
COPY --from=build /src/backend/server/build/install/server/ /app/
USER app
EXPOSE 8080
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-prod.xml"
ENTRYPOINT ["/app/bin/server"]
