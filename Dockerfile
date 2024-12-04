# Base image
FROM eclipse-temurin:21-jre

# Arguments for the build
ARG BUILD_DATE

# Metadata
LABEL authors="Aladin Iseni <aladin.iseni@heig-vd.ch>, Arthur Jacobs <arthur.jacobs@heig-vd.ch>"
LABEL maintainer="Aladin Iseni <aladin.iseni@heig-vd.ch>, Arthur Jacobs <arthur.jacobs@heig-vd.ch>"

# Connect this image in the GitHub container registry to the project's repository
LABEL org.opencontainers.image.source=https://github.com/HEIG-VD-DAI-Iseni-Jacobs/no-tion
LABEL org.label-schema.build-date=$BUILD_DATE

WORKDIR /app
COPY target/no-tion-1.0-SNAPSHOT.jar /app/no-tion-1.0-SNAPSHOT.jar

# Run the application
ENTRYPOINT ["java", "-jar", "no-tion-1.0-SNAPSHOT.jar"]

CMD ["--help"]