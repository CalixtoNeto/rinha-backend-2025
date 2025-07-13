FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Copia wrapper, pom e código
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
COPY src ./src

# Dá permissão e roda o wrapper
RUN chmod +x mvnw && \
    ./mvnw clean generate-sources install -DskipTests -B

FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 9999
ENTRYPOINT ["java","-jar","/app/app.jar"]