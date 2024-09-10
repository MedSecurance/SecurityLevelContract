FROM ubuntu:23.04
LABEL authors="narow"

RUN apt-get update && apt-get upgrade -y
RUN apt install -y openjdk-21-jdk maven openssl curl

RUN mkdir signingTool
WORKDIR signingTool
COPY src ./src
COPY entrypoint.sh .
COPY pom.xml .
WORKDIR src/main/resources
RUN sh generateKeys.sh

WORKDIR /signingTool
RUN mvn package

ENTRYPOINT ["sh", "entrypoint.sh"]