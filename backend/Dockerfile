FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /workspace

COPY pom.xml ./
RUN mvn -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN useradd --create-home --shell /usr/sbin/nologin simulab
COPY --from=builder /workspace/target/simulab-backend-0.1.0-SNAPSHOT.jar /app/simulab-backend.jar
RUN chown -R simulab:simulab /app

USER simulab
EXPOSE 8080

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/simulab-backend.jar"]
