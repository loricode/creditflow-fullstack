# syntax=docker/dockerfile:1.7
# Una sola compilación Maven para todos los módulos; cada servicio es un target.
#   docker build -f infrastructure/docker/backend.Dockerfile --target api .

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /src
# POMs primero: la descarga de dependencias queda en caché mientras no cambien
COPY backend/pom.xml backend/pom.xml
COPY crediflow-database/pom.xml crediflow-database/pom.xml
COPY backend/crediflow-shared/pom.xml backend/crediflow-shared/pom.xml
COPY backend/crediflow-api/pom.xml backend/crediflow-api/pom.xml
COPY backend/crediflow-publisher/pom.xml backend/crediflow-publisher/pom.xml
COPY backend/crediflow-worker/pom.xml backend/crediflow-worker/pom.xml
COPY backend/crediflow-core-bancario/pom.xml backend/crediflow-core-bancario/pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f backend/pom.xml dependency:go-offline -DexcludeReactor=true || true

COPY crediflow-database crediflow-database
COPY backend backend
RUN --mount=type=cache,target=/root/.m2 mvn -B -q -f backend/pom.xml package -DskipTests

FROM eclipse-temurin:17-jre-alpine AS runtime
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
USER app
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]

FROM runtime AS api
COPY --from=build /src/backend/crediflow-api/target/crediflow-api.jar /app/app.jar
EXPOSE 8080

FROM runtime AS publisher
COPY --from=build /src/backend/crediflow-publisher/target/crediflow-publisher.jar /app/app.jar
EXPOSE 8081

FROM runtime AS worker
COPY --from=build /src/backend/crediflow-worker/target/crediflow-worker.jar /app/app.jar
EXPOSE 8082

FROM runtime AS core-bancario
COPY --from=build /src/backend/crediflow-core-bancario/target/crediflow-core-bancario.jar /app/app.jar
EXPOSE 8090
