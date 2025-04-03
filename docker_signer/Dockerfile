FROM ubuntu:24.04
LABEL authors="narow"

RUN apt-get update && apt-get upgrade -y
RUN apt install -y openjdk-21-jdk maven openssl curl dnsutils

RUN mkdir signer
WORKDIR signer
COPY ./pom.xml .
COPY ./mvnw .
COPY ./src ./src
COPY  ./.mvn/wrapper/maven-wrapper.properties  ./.mvn/wrapper/maven-wrapper.properties
#RUN mvn -Dmaven.multiModuleProjectDirectory=/signer package
RUN chmod u+x mvnw
RUN nslookup repo.maven.apache.org
RUN curl  https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.tar.gz
RUN ./mvnw package

ENTRYPOINT ["java", "-jar", "target/SigningInterface-0.0.1-SNAPSHOT.jar"]