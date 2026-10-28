FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B clean package -DskipTests

FROM eclipse-temurin:21-jre AS runtime
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --shell /usr/sbin/nologin appuser
WORKDIR /app
COPY --from=build /build/target/*.jar app.jar
RUN chown -R appuser:appuser /app
USER appuser
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
