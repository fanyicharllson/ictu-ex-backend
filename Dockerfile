FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Use a more specific pattern to ensure you get the actual bootable JAR
# and check that your Gradle build is producing the 'plain' vs 'boot' jar correctly.
COPY ictu-ex-app/build/libs/ictu-ex-app-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Xms256m", "-Xmx512m", "-jar", "app.jar"]
