# Base image
FROM eclipse-temurin:21-jre

# Connect this image in the GitHub container registry to the project's repository
LABEL org.opencontainers.image.source=https://github.com/HEIG-VD-DAI-Iseni-Jacobs/no-tion

# Set the working directory
WORKDIR /app

# Copy the jar file
COPY target/no-tion-1.0-SNAPSHOT.jar /app/no-tion-1.0-SNAPSHOT.jar

# Set the entrypoint
ENTRYPOINT ["java", "-jar", "no-tion-1.0-SNAPSHOT.jar"]

# Set the default command
CMD ["--help"]