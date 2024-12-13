pwd
ls
cp caWorkspace/consumer.p12 /consumer_key/key.p12
ls /consumer_key
cp caWorkspace/provider.p12 /provider_key/key.p12
ls /provider_key
cp trust-anchors.jks /trust_anchors
cp rootWorkspace/rootcert.pem /root_cert/
cd /caWorkspace
openssl ocsp -index index.txt -port 8888 -rsigner localhost.p12 -passin pass:localhost_key -rkey localhost_key.pem -CA ca_signed_cert.pem -text -out log.txt &
echo ocsp is started
cd /crlServer
python3 -m http.server 8089