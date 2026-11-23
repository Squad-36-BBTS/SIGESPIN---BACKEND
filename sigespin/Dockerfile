# Imagem do backend SIGESPIN para o Render.
# No Render, use "Root Directory" = sigespin, para que este Dockerfile e o código fiquem no contexto.

# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Copia primeiro só o que define as dependências, para o Docker reaproveitar o cache entre builds.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src src
# Os testes rodam na CI e localmente com PostgreSQL; aqui só empacotamos.
RUN ./mvnw -B -q package -DskipTests && cp target/sigespin-*.jar app.jar

# ---------- Etapa 2: executar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Usuário sem privilégios: o processo não roda como root.
RUN useradd --system --uid 10001 sigespin
COPY --from=build /app/app.jar app.jar
USER sigespin

# O plano gratuito do Render tem 512 MB. A JVM usa no máximo 75% disso para heap,
# com coletor serial (menos memória) e pilhas de thread menores.
# O relógio do Render é UTC; as datas do domínio (LocalDateTime) seguem o horário de Brasília.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError -Duser.timezone=America/Sao_Paulo"

# O Render define PORT; o application.properties usa ${PORT:8080}.
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
