# Estágio 1: Build da Aplicação
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia o POM para baixar as dependências e usar o cache do Docker
COPY pom.xml .
# Baixa as dependências do projeto para agilizar os builds seguintes (modo offline)
RUN mvn dependency:go-offline -B

# Copia o código-fonte da aplicação
COPY src ./src

# Executa o build da aplicação, ignorando os testes unitários para ser mais rápido no deploy
RUN mvn clean package -DskipTests

# Estágio 2: Execução (Imagem enxuta apenas com o JRE)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Instala ferramentas básicas como curl e bibliotecas C++ nativas para OpenCV (libstdc++, gcompat, libgomp)
RUN apk add --no-cache curl libstdc++ gcompat libgomp

# Por segurança, rodar como usuário não-root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copia o arquivo .jar gerado no primeiro estágio
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta que o Spring Boot vai usar (por padrão 8080)
EXPOSE 8080

# Parâmetros de otimização de memória para contêineres e inicialização
ENTRYPOINT ["java", "-XX:+UseZGC", "-XX:+ZGenerational", "-jar", "app.jar"]
