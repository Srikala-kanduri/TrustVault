FROM node:22-alpine AS frontend-build
WORKDIR /frontend
COPY trustvault-frontend/package.json trustvault-frontend/package-lock.json ./
RUN npm ci
COPY trustvault-frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-21 AS backend-build
WORKDIR /workspace/trustvault
COPY trustvault/pom.xml ./pom.xml
COPY trustvault/src ./src
COPY --from=frontend-build /frontend/dist ./src/main/resources/static
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 10001 --create-home trustvault
COPY --from=backend-build /workspace/trustvault/target/trustvault-0.0.1-SNAPSHOT.jar /app/app.jar
RUN chown -R trustvault:trustvault /app
USER trustvault
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
