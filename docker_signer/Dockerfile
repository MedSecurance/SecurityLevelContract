FROM ubuntu:25.04
LABEL authors="narow"

RUN apt-get update
RUN apt-get upgrade -y
RUN apt install -y openjdk-21-jdk maven openssl curl dnsutils

RUN mkdir signer
WORKDIR signer
COPY ./pom.xml .
COPY ./src ./src
RUN mvn package

ENTRYPOINT ["java", "-jar", "target/SigningInterface-0.0.1-SNAPSHOT.jar"]