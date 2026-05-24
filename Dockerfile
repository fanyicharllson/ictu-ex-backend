FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Using wildcards matches the compiled jar regardless of exact snapshot/release naming strings
COPY ictu-ex-app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
