#cp caPEMPassphrase.txt caPEMPassphrase_copy.txt

rm caWorkspace/index.txt
rm rootWorkspace/ca_signed_cert.pem
rm caWorkspace/signing_request_consumer.csr
rm caWorkspace/index.txt.attr
rm rootWorkspace/serial.txt.old
rm caWorkspace/signing_request_provider.csr
rm caWorkspace/serial.txt
rm caWorkspace/localhost_key.pem
rm caWorkspace/localhost.p12
rm caWorkspace/index.txt.attr.old
rm caWorkspace/index.txt.old
rm caWorkspace/02.pem
rm caWorkspace/provider_signed_cert.pem
rm rootWorkspace/index.txt
rm caWorkspace/signing_request_localhost.csr
rm caWorkspace/log.txt
rm caWorkspace/provider_key.pem
rm rootWorkspace/index.txt.old
rm caWorkspace/provider.p12
rm caWorkspace/serial.txt.old
rm rootWorkspace/signing_request_ca.csr
rm rootWorkspace/index.txt.attr
rm rootWorkspace/rootcert.pem
rm rootWorkspace/ca_key.pem
rm caWorkspace/consumer_signed_cert.pem
rm caWorkspace/03.pem
rm rootWorkspace/serial.txt
rm rootWorkspace/rootkey.pem
rm caWorkspace/consumer.p12
rm caWorkspace/consumer_key.pem
rm caWorkspace/ca_key.pem
rm rootWorkspace/01.pem
rm rootWorkspace/crl.pem
rm caWorkspace/01.pem
rm caWorkspace/ca_signed_cert.pem

#Create root
pwd
ls
cd rootWorkspace
openssl req -x509 -config openssl-ca.cnf -days 365 -newkey rsa:4096 -sha256 -out rootcert.pem -passout file:rootPEMPassphrase.txt -outform PEM -subj "/C=ES/ST=Catalunya/L=Barcelona/O=UPC/OU=DMAG/CN=rootCA"
touch index.txt
echo '01' > serial.txt
echo '01' > crlnumber

#Create CA
openssl genpkey -algorithm RSA -out ca_key.pem -pass pass:ca_key -aes256
openssl req -new -key ca_key.pem -out signing_request_ca.csr -passin pass:ca_key -subj "/C=ES/ST=testSt/L=testL/O=testO/OU=testOU/CN=Certificate Authority"
openssl ca -batch -config openssl-ca.cnf -policy signing_policy  -out ca_signed_cert.pem -passin file:rootPEMPassphrase.txt -in signing_request_ca.csr -extensions v3_ca

openssl ca -config openssl-ca.cnf -passin file:rootPEMPassphrase.txt -gencrl -out crl.pem

cp ca_key.pem ../caWorkspace
cp ca_signed_cert.pem ../caWorkspace
mkdir ../crlServer
cp crl.pem ../crlServer
cp rootcert.pem ../crlServer


cd ../caWorkspace
touch index.txt
echo '01' > serial.txt

#Generate key for consumer (i.e. signer 1)
openssl genpkey -algorithm RSA -out consumer_key.pem -pass pass:consumer_key -aes256
openssl req -new -key consumer_key.pem -out signing_request_consumer.csr -passin pass:consumer_key -subj "/C=ES/ST=Catalunya/L=Barcelona/O=UPC/OU=DMAG/CN=consumer"
openssl ca -batch -config openssl-ca.cnf -policy signing_policy  -out consumer_signed_cert.pem -passin pass:ca_key -in signing_request_consumer.csr -extensions final_user
openssl pkcs12 -export -out consumer.p12 -inkey consumer_key.pem -in consumer_signed_cert.pem -certfile ca_signed_cert.pem -passin pass:consumer_key -password pass:key

#Generate key for consumer (i.e. signer 2)
openssl genpkey -algorithm RSA -out provider_key.pem -pass pass:provider_key -aes256
openssl req -new -key provider_key.pem -out signing_request_provider.csr -passin pass:provider_key -subj "/C=ES/ST=Catalunya/L=Barcelona/O=UPC/OU=DMAG/CN=provider"
openssl ca -batch -config openssl-ca.cnf -policy signing_policy  -out provider_signed_cert.pem  -passin pass:ca_key -in signing_request_provider.csr -extensions final_user
openssl pkcs12 -export -out provider.p12 -inkey provider_key.pem -in provider_signed_cert.pem -certfile ca_signed_cert.pem -passin pass:provider_key -password pass:key

#Generate certificate for OCSP responder
openssl genpkey -algorithm RSA -out localhost_key.pem -pass pass:localhost_key -aes256
openssl req -new -key localhost_key.pem -out signing_request_localhost.csr -passin pass:localhost_key -subj "/C=ES/ST=Catalunya/L=Barcelona/O=UPC/OU=DMAG/CN=localhost"
openssl ca -batch -config openssl-ca.cnf -policy signing_policy  -out localhost.p12  -passin pass:ca_key -in signing_request_localhost.csr -extensions ocsp_cert

#Prepare data for execution
cd ..
curl -O http://dss.nowina.lu/pki-factory/keystore/trust-anchors.jks
keytool -noprompt -importcert -file ./rootWorkspace/rootcert.pem -keystore trust-anchors.jks -alias myCA -storepass ks-password

cp caWorkspace/consumer.p12 .
cp caWorkspace/localhost.p12 .
cp caWorkspace/provider.p12 .

#       openssl req -x509 -config openssl-ca.cnf -days 365 -newkey rsa:4096 -sha256 -out cacert.pem -passout file:caPEMPassphrase.txt -outform PEM -subj "/C=ES/ST=Catalunya/L=Barcelona/O=UPC/OU=DMAG/CN=Certificate Authority"

#       touch index.txt
#       echo '01' > serial.txt

#openssl pkcs12 -export -out cacert.p12 -in cacert.pem -inkey cakey.pem -passout file:caPEMPassphrase.txt -passin file:caPEMPassphrase.txt







#Start OCSP responder (caWorkspace)
#       openssl ocsp -index index.txt -port 8888 -rsigner localhost.p12 -rkey localhost_key.pem -CA ca_signed_cert.pem -text -out log.txt
#Start CRL responder (crlServer)
#     python3 -m http.server 8089