# 1) Imagem base com Java 21 para build
FROM eclipse-temurin:21-jdk-jammy AS build

# 2) Diretório de trabalho
WORKDIR /app

# 3) Copia o pom.xml e faz download de dependências (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 4) Copia o código-fonte e builda o jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# 5) Stage de runtime: apenas o JAR, também em Java 21
FROM eclipse-temurin:21-jdk-jammy
WORKDIR /app

# Copia o jar gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta HTTP da aplicação
EXPOSE 9999

# Entry point
ENTRYPOINT ["java","-jar","/app/app.jar"]
