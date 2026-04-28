FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml lombok.config pmd.xml spotbugs-excludes.xml ./
COPY scrapper/pom.xml scrapper/
COPY bot/pom.xml bot/
COPY ai-agent/pom.xml ai-agent/
COPY build-report-aggregate/pom.xml build-report-aggregate/
RUN chmod +x mvnw && ./mvnw -pl bot -am dependency:go-offline -B -DskipTests

COPY scrapper/src scrapper/src/
COPY bot/src bot/src/
COPY migrations migrations/
RUN ./mvnw -pl bot -am package -DskipTests -B

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/bot/target/bot-*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
