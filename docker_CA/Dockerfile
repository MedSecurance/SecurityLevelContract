FROM ubuntu:24.04
LABEL authors="narow"

RUN apt-get update && apt-get upgrade -y
RUN apt install -y openjdk-21-jdk maven openssl curl

#RUN mkdir signingTool
#WORKDIR signingTool
ADD caWorkspace ./caWorkspace
ADD rootWorkspace ./rootWorkspace
COPY generateKeys.sh .
COPY entrypoint.sh .
RUN sh generateKeys.sh

ENTRYPOINT ["sh", "entrypoint.sh"]