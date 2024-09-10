cd /signingTool/src/main/resources/caWorkspace
openssl ocsp -index index.txt -port 8888 -rsigner localhost.p12 -passin pass:localhost_key -rkey localhost_key.pem -CA ca_signed_cert.pem -text -out log.txt &
cd /signingTool/src/main/resources/crlServer
python3 -m http.server 8089 &
cd /signingTool
java -jar target/DssCookbookTest-1.0-SNAPSHOT.jar