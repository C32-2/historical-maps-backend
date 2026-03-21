FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradle gradle
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY gradle.properties gradle.properties
COPY src src

RUN chmod +x gradlew && ./gradlew --no-daemon installDist

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /app/build/install/historical-maps-backend /app/

EXPOSE 8080

CMD ["/app/bin/historical-maps-backend"]
