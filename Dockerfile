# ---- Build stage: sukompiliuojam JAR su Gradle wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Gradle wrapper + konfigai (cache)
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew
RUN ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

# Šaltinis
COPY src src

# Sukuriam fat JAR
RUN ./gradlew --no-daemon clean bootJar

# ---- Runtime stage: lengvas JRE image ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# JAR
COPY --from=build /app/build/libs/*.jar /app/app.jar

# (pasirinktinai) HEALTHCHECK, jei įjungsi actuator/health
# HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
#   CMD wget -qO- http://localhost:8080/actuator/health | grep '"status":"UP"' || exit 1

EXPOSE 8003
ENTRYPOINT ["java","-jar","/app/app.jar"]
