FROM eclipse-temurin:26-jdk AS build
WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src src
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:26-jre
WORKDIR /app

RUN addgroup --system rdas && adduser --system --ingroup rdas rdas
COPY --from=build /workspace/target/rdas-0.0.1-SNAPSHOT.jar /app/rdas.jar
RUN mkdir -p /app/data && chown -R rdas:rdas /app

USER rdas
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/rdas.jar"]
