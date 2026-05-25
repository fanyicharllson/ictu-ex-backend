FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Using an explicit suffix matching rule bypassing syntax parsing exceptions
COPY ictu-ex-app/build/libs/*-boot.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
