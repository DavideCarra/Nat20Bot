# --- Maven Build ---
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy jar in the container
COPY libs/finite_state_machine.jar /tmp/

# Install custom jar
RUN mvn install:install-file \
    -Dfile=/tmp/finite_state_machine.jar \
    -DgroupId=com.statemachine \
    -DartifactId=statemachine \
    -Dversion=1.0 \
    -Dpackaging=jar

# Copy project
COPY pom.xml .
COPY src ./src

# Compile and create jar bot
RUN mvn clean package -DskipTests

# --- Image ---
FROM eclipse-temurin:17
WORKDIR /app

# Copy final jar
COPY --from=build /app/target/nat20bot-1.0-SNAPSHOT.jar app.jar

# env variable
ENV BOT_TOKEN=changeme

# bot start
ENTRYPOINT ["java","-jar","app.jar"]