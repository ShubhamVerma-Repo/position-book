# --- Build stage ---
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

COPY pom.xml .
COPY src ./src

# -DskipTests is safe here specifically because CI (mvn clean verify, including the
# JaCoCo coverage gate) already ran and passed before this Docker build would ever
# execute in a real pipeline - this stage only packages already-verified code, it
# does not re-prove correctness.
RUN mvn clean package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=build /build/target/position-book-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
