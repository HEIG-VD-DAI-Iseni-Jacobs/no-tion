# Base image
FROM eclipse-temurin:21-jre

# Arguments for the build
ARG BUILD_DATE

# Metadata
LABEL org.label-schema.name="no-tion"
LABEL org.label-schema.description="No-tion is a tool to store notes on a server using a simple protocol."
LABEL org.label-schema.url="https://github.com/HEIG-VD-DAI-Iseni-Jacobs/no-tion"
LABEL authors="Aladin Iseni <aladin.iseni@heig-vd.ch>, Arthur Jacobs <arthur.jacobs@heig-vd.ch>"
LABEL maintainer="Aladin Iseni <aladin.iseni@heig-vd.ch>, Arthur Jacobs <arthur.jacobs@heig-vd.ch>"
LABEL org.label-schema.version="0.1.0"


# Connect this image in the GitHub container registry to the project's repository
LABEL org.opencontainers.image.source=https://github.com/HEIG-VD-DAI-Iseni-Jacobs/no-tion
LABEL org.label-schema.build-date=$BUILD_DATE

WORKDIR /app
COPY target/no-tion-1.0-SNAPSHOT.jar /app/no-tion-1.0-SNAPSHOT.jar

# Run the application
ENTRYPOINT ["java", "-jar", "no-tion-1.0-SNAPSHOT.jar"]

CMD ["--help"]