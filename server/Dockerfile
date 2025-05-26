FROM openjdk:17-jdk-slim
WORKDIR /app
COPY server-1.0-SNAPSHOT.jar .
COPY output.json .
CMD ["java", "-jar", "server-1.0-SNAPSHOT.jar", "output.json"]