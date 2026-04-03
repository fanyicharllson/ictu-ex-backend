FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY ictu-ex-app/build/libs/*-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]