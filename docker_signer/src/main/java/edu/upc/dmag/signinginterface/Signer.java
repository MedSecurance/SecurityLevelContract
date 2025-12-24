package edu.upc.dmag.signinginterface;

import eu.europa.esig.dss.asic.common.ASiCContent;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESContainerExtractor;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESSignatureParameters;
import eu.europa.esig.dss.asic.xades.signature.ASiCWithXAdESService;
import eu.europa.esig.dss.diagnostic.AbstractTokenProxy;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.enumerations.ASiCContainerType;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.*;
import eu.europa.esig.dss.model.x509.CertificateToken;
import eu.europa.esig.dss.service.crl.OnlineCRLSource;
import eu.europa.esig.dss.service.http.commons.CommonsDataLoader;
import eu.europa.esig.dss.service.http.commons.FileCacheDataLoader;
import eu.europa.esig.dss.service.http.commons.TimestampDataLoader;
import eu.europa.esig.dss.service.http.proxy.ProxyConfig;
import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
import eu.europa.esig.dss.service.tsp.OnlineTSPSource;
import eu.europa.esig.dss.spi.DSSUtils;
import eu.europa.esig.dss.spi.client.http.DataLoader;
import eu.europa.esig.dss.spi.x509.CertificateSource;
import eu.europa.esig.dss.spi.x509.CommonTrustedCertificateSource;
import eu.europa.esig.dss.spi.x509.KeyStoreCertificateSource;
import eu.europa.esig.dss.spi.x509.tsp.TSPSource;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.*;

@Slf4j
@RequiredArgsConstructor
@Component
public class Signer {
    private final ProjectsContractStatus projectsContractStatus;
    private static final char[] PKI_FACTORY_KEYSTORE_PASSWORD = {'k', 's', '-', 'p', 'a', 's', 's', 'w', 'o', 'r', 'd'};

    private static final String PKI_FACTORY_HOST = "http://dss.nowina.lu/pki-factory/";
    private static final String TSA_ROOT_PATH = "/tsa/";
    private static final String DEFAULT_TSA_DATE_FORMAT = "yyyy-MM-dd-HH-mm";
    protected static final String GOOD_TSA = "good-tsa";

    private static final int TIMEOUT_MS = 10000;
    private static final String PKI_FACTORY_KEYSTORE_PATH = "/keystore/";
    protected static final String GOOD_USER = "good-user";

    private static CommonTrustedCertificateSource trustedCertificateSource;

    protected static CertificateSource getOnlineTrustedCertificateSource() {
        byte[] trustedStoreContent = getOnlineKeystoreContent("trust-anchors.jks");
        KeyStoreCertificateSource keystore = new KeyStoreCertificateSource(new ByteArrayInputStream(trustedStoreContent), "JKS", PKI_FACTORY_KEYSTORE_PASSWORD);
        CommonTrustedCertificateSource trustedCertificateSource = new CommonTrustedCertificateSource();
        trustedCertificateSource.importAsTrusted(keystore);
        return trustedCertificateSource;
    }

    protected static Path removeSignatures(String inputXML) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        ByteArrayInputStream input = new ByteArrayInputStream(inputXML.getBytes(StandardCharsets.UTF_8));
        Document document = builder.parse(input);

        NodeList signaturesList = document.getElementsByTagName("signatures");
        if (signaturesList.getLength() > 0) {
            Node signaturesNode = signaturesList.item(0);
            while (signaturesNode.hasChildNodes()) {
                signaturesNode.removeChild(signaturesNode.getFirstChild());
            }
        }

        DOMSource source = new DOMSource(document);
        Path tempFile = Files.createTempFile("xml-output-", ".xml");
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        try (OutputStream out = Files.newOutputStream(tempFile)) {
            transformer.transform(source, new StreamResult(out));
        }

        return tempFile;
    }

    protected static CertificateSource getModifiedOnlineTrustedCertificateSource() throws IOException {
        KeyStoreCertificateSource keystore = new KeyStoreCertificateSource(new File("/trust_anchors/trust-anchors.jks"), "JKS", PKI_FACTORY_KEYSTORE_PASSWORD);
        CommonTrustedCertificateSource trustedCertificateSource = new CommonTrustedCertificateSource();
        trustedCertificateSource.importAsTrusted(keystore);
        return trustedCertificateSource;
    }

    protected static CertificateSource getTrustedCertificateSource() throws IOException {
        if (trustedCertificateSource == null) {
            trustedCertificateSource = new CommonTrustedCertificateSource();
        }

        getModifiedOnlineTrustedCertificateSource().getCertificates().forEach( certificate -> {
            logCertificate("Adding trusted certificates: ", certificate);

            trustedCertificateSource.addCertificate(certificate);
        });
        return trustedCertificateSource;
    }

    private static void logCertificate(String introMessage, CertificateToken certificate) {
        log.debug("""
                {}:\s
                 \
                - Subject: {}\s
                - Issuer: {}\s
                - hash: {}\s
                - DSS ID: {}\s
                - certificate hash code: {}
                - """,
                introMessage,
                certificate.getSubject().getCanonical(),
                certificate.getIssuer().getCanonical(),
                certificate.hashCode(),
                certificate.getDSSIdAsString(),
                certificate.getCertificate().hashCode());
    }

    protected static byte[] getOnlineKeystoreContent(String keystoreName) {
        DataLoader dataLoader = getFileCacheDataLoader();
        String keystoreUrl = PKI_FACTORY_HOST + PKI_FACTORY_KEYSTORE_PATH + keystoreName;
        return dataLoader.get(keystoreUrl);
    }

    protected static ProxyConfig getProxyConfig() {
        return null;
    }

    protected static DataLoader getFileCacheDataLoader() {
        FileCacheDataLoader cacheDataLoader = new FileCacheDataLoader();
        CommonsDataLoader dataLoader = new CommonsDataLoader();
        dataLoader.setProxyConfig(getProxyConfig());
        dataLoader.setTimeoutConnection(TIMEOUT_MS);
        dataLoader.setTimeoutSocket(TIMEOUT_MS);
        cacheDataLoader.setDataLoader(dataLoader);
        cacheDataLoader.setFileCacheDirectory(new File("target"));
        cacheDataLoader.setCacheExpirationTime(3600000L);
        return cacheDataLoader;
    }

    private static OnlineTSPSource getOnlineTSPSourceByUrl(String tsaUrl) {
        OnlineTSPSource tspSource = new OnlineTSPSource(tsaUrl);
        TimestampDataLoader dataLoader = new TimestampDataLoader();
        dataLoader.setTimeoutConnection(TIMEOUT_MS);
        dataLoader.setTimeoutSocket(TIMEOUT_MS);
        dataLoader.setProxyConfig(getProxyConfig());
        tspSource.setDataLoader(dataLoader);
        return tspSource;
    }

    private static String getTsaUrl(String tsaName) {
        return getTsaUrl(tsaName, null);
    }

    private static String getTsaUrl(String tsaName, Date date) {
        StringBuilder sb = new StringBuilder();
        sb.append(PKI_FACTORY_HOST);
        sb.append(TSA_ROOT_PATH);
        if (date != null) {
            String dateString = DSSUtils.formatDateWithCustomFormat(date, DEFAULT_TSA_DATE_FORMAT);
            sb.append(dateString);
            sb.append('/');
        }
        sb.append(tsaName);
        return sb.toString();
    }

    protected static OnlineTSPSource getOnlineTSPSourceByName(String tsaName) {
        return getOnlineTSPSourceByUrl(getTsaUrl(tsaName));
    }

    protected static void storeBytes(final byte[] bytes, final String path) throws IOException {
        try(OutputStream stream = new FileOutputStream(path)){
            stream.write(bytes);
        };
    }



    protected static TSPSource getOnlineTSPSource() {
        return getOnlineTSPSourceByName(GOOD_TSA);
    }

    protected DSSDocument sign(String project, Map<KnownDocuments, File> content) throws Exception {
        String pathToKey = "/key/key.p12";
        char[] keyForCertificate = {'k', 'e', 'y'};

        log.debug("Starting signing process for project: {}", project);

        Map<KnownDocuments, File> validContent = new HashMap<>();
        for (var el : content.entrySet()) {

            KnownDocuments knownDocument = el.getKey();
            boolean hashIsValid = dataChildHashIsValid(project, el.getValue(), knownDocument);
            if (hashIsValid) {
                validContent.put(el.getKey(), el.getValue());
                log.debug("Document {} passed hash validation and will be included in the signature.", knownDocument.getName());
            }
        }

        log.debug("Total documents to be signed after validation: {}", validContent.size());
        List<DSSDocument> documentsToBeSigned = new ArrayList<>();
        for (var entry : content.entrySet()) {
            var documentToSign = new FileDocument(entry.getValue());
            documentToSign.setName(entry.getKey().getName());
            documentsToBeSigned.add(documentToSign);
        }
        log.debug("Prepared {} documents for signing.", documentsToBeSigned.size());

        ASiCWithXAdESSignatureParameters parameters = new ASiCWithXAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_LTA);
        parameters.aSiC().setContainerType(ASiCContainerType.ASiC_E);
        parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);

        log.debug("Signature parameters set: \n" +
                        "Level - {}, \n" +
                        "Container Type - {}, \n" +
                        "Digest Algorithm - {}",
                parameters.getSignatureLevel(),
                parameters.aSiC().getContainerType(),
                parameters.getDigestAlgorithm()
        );
        try (//SignatureTokenConnection signingToken = getUserPkcs12Token()
             KeyStoreSignatureTokenConnection signingToken = new KeyStoreSignatureTokenConnection(
                     pathToKey,
                     "PKCS12",
                     new KeyStore.PasswordProtection(keyForCertificate)
             )
        ) {
            log.debug("Loaded signing token from keystore: {}", pathToKey);
            DSSPrivateKeyEntry privateKey = signingToken.getKeys().get(0);

            logCertificate("certificate to sign", privateKey.getCertificate());
            for (var certificateInChain: privateKey.getCertificateChain()){
                logCertificate("certificate in chain", certificateInChain);
            };

            parameters.setSigningCertificate(privateKey.getCertificate());
            parameters.setCertificateChain(privateKey.getCertificateChain());

            CommonCertificateVerifier commonCertificateVerifier = getCommonCertificateVerifier();
            ASiCWithXAdESService service = new ASiCWithXAdESService(commonCertificateVerifier);
            service.setTspSource(getOnlineTSPSource());

            log.debug("Starting the signing operation for {} documents.", documentsToBeSigned.size());
            ToBeSigned dataToSign = service.getDataToSign(documentsToBeSigned, parameters);
            log.debug("Data to sign prepared. Digest Algorithm: {}", parameters.getDigestAlgorithm());

            DigestAlgorithm digestAlgorithm = parameters.getDigestAlgorithm();
            log.debug("Signing the data using the private key.");
            SignatureValue signatureValue = signingToken.sign(dataToSign, digestAlgorithm, privateKey);
            log.debug("Data signed successfully. Generating the signed document.");

            DSSDocument signed = service.signDocument(documentsToBeSigned, parameters, signatureValue);
            log.debug("Document signed successfully.");
            return signed;
        }
    }

    private void registerSignatures(String project, String pathToKey, char[] keyForCertificate, Set<KnownDocuments> includedDocuments) throws CertificateException, IOException {
        try (KeyStoreSignatureTokenConnection t = new KeyStoreSignatureTokenConnection(pathToKey, "PKCS12", new KeyStore.PasswordProtection(keyForCertificate))) {
            DSSPrivateKeyEntry entry = t.getKeys().get(0);
            byte[] encoded = entry.getCertificate().getEncoded();
            java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
            java.security.cert.X509Certificate x509 = (java.security.cert.X509Certificate) cf.generateCertificate(new ByteArrayInputStream(encoded));
            String dn = x509.getSubjectX500Principal().getName();
            String cn = "";
            String o = "";
            for (String part : dn.split(",")) {
                part = part.trim();
                if (part.startsWith("CN=")) cn = part.substring(3);
                if (part.startsWith("O=")) o = part.substring(2);
            }
            log.info("CN={}, O={}", cn, o);

            for (var document: includedDocuments) {
                projectsContractStatus.registerNewSignature(project, document, cn, o);
            }
        }
    }

    private boolean dataChildHashIsValid(String project, File fileToTest, KnownDocuments documentToTest) throws NoSuchAlgorithmException, IOException {
        String hash = Utils.sha256(fileToTest);
        return isHashValid(project, hash, documentToTest);
    }

    private boolean isHashValid(String project, String hash, KnownDocuments document) throws NoSuchAlgorithmException {
        boolean hashIsValid = false;

        if (projectsContractStatus.checkDocumentHash(
                project,
                document,
                hash
        )){
            hashIsValid = true;
        } else {
            log.warn("Document {} not included in signatures because hash mismatch (calculated: {}, expected: {})",
                    document.getName(),
                    hash,
                    projectsContractStatus.getDocumentHash(project, document)
            );
        }
        return hashIsValid;
    }

    private static String document_to_string(Document mergedResult) throws IOException, TransformerException {
        DOMSource source = new DOMSource(mergedResult);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.transform(source, result);

        return writer.toString();
    }

    private static Document merge_signatures(List<String> outputPaths) throws ParserConfigurationException, IOException, SAXException {
        Document finalDocument = null;
        for (String outputPath : outputPaths) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(outputPath);

            if (finalDocument == null) {
                finalDocument = doc;
            } else {
                Node signatureNode = doc.getDocumentElement().getElementsByTagName("signatures").item(0).getFirstChild();
                finalDocument.getDocumentElement().getElementsByTagName("signatures").item(0).appendChild(
                        finalDocument.importNode(signatureNode, true)
                );
            }
        }
        return finalDocument;
    }

    private static DSSDocument basicSignDocument(
            DSSDocument toSignDocument,
            String pathToKey,
            char[] keyForCertificate
    ) throws IOException {
        DSSDocument signedDocument = null;
        try (//SignatureTokenConnection signingToken = getUserPkcs12Token()
             KeyStoreSignatureTokenConnection signingToken = new KeyStoreSignatureTokenConnection(
                     pathToKey,
                     "PKCS12",
                     new KeyStore.PasswordProtection(keyForCertificate)
             )
        ) {

            DSSPrivateKeyEntry privateKey = signingToken.getKeys().get(0);
            XAdESSignatureParameters parameters = new XAdESSignatureParameters();
            parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_B);
            parameters.setSignaturePackaging(SignaturePackaging.ENVELOPED);
            parameters.setDigestAlgorithm(DigestAlgorithm.SHA256);
            parameters.setSigningCertificate(privateKey.getCertificate());
            parameters.setCertificateChain(privateKey.getCertificateChain());
            parameters.setXPathLocationString("/root/signatures");

            XAdESService service = new XAdESService(new CommonCertificateVerifier());
            ToBeSigned dataToSign = service.getDataToSign(toSignDocument, parameters);
            SignatureValue signatureValue = signingToken.sign(dataToSign, parameters.getDigestAlgorithm(), privateKey);
            signedDocument = service.signDocument(toSignDocument, parameters, signatureValue);
        }
        return signedDocument;
    }

    private static DSSDocument extendToLTA(DSSDocument ltLevelDocument) throws IOException {
        var parameters = new XAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_LTA);

        return performExtension(ltLevelDocument, parameters);
    }

    private static DSSDocument extendToLT(DSSDocument tLevelSignature) throws IOException {
        // Create signature parameters with target extension level
        XAdESSignatureParameters parameters = new XAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_LT);

        return performExtension(tLevelSignature, parameters);
    }

    private static DSSDocument performExtension(DSSDocument tLevelSignature, XAdESSignatureParameters parameters) throws IOException {
        // Create a CertificateVerifier with revocation sources for -LT level extension
        CommonCertificateVerifier certificateVerifier = getCommonCertificateVerifier();

        // Init service for signature augmentation
        XAdESService xadesService = getXAdESService(certificateVerifier);

        // Extend signature
        return xadesService.extendDocument(tLevelSignature, parameters);
    }

    private static XAdESService getXAdESService(CommonCertificateVerifier certificateVerifier) {
        XAdESService xadesService;
        xadesService = new XAdESService(certificateVerifier);
        xadesService.setTspSource(getOnlineTSPSource());
        return xadesService;
    }

    private static CommonCertificateVerifier getCommonCertificateVerifier() throws IOException {
        CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();

        // init revocation sources for CRL/OCSP requesting
        certificateVerifier.setCrlSource(new OnlineCRLSource());
        certificateVerifier.setOcspSource(new OnlineOCSPSource());

        // Trust anchors should be defined for revocation file requesting
        certificateVerifier.setTrustedCertSources(getTrustedCertificateSource());
        return certificateVerifier;
    }

    private static DSSDocument extendToT(DSSDocument signedDocument) throws IOException {

        // Create signature parameters with target extension level
        XAdESSignatureParameters parameters = new XAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_T);

        return performExtension(signedDocument, parameters);
    }

    protected static SignedDocumentValidator getValidator(DSSDocument signedDocument) {
        SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(signedDocument);
        validator.setCertificateVerifier(new CommonCertificateVerifier());
        return validator;
    }


    public Boolean validate(String project, String content) throws IOException {
        DSSDocument signedDocument = new InMemoryDocument(content.getBytes());
        SignedDocumentValidator validator = getValidator(signedDocument);

        CertificateVerifier certificateVerifier = new CommonCertificateVerifier();
        CommonTrustedCertificateSource  trustedCertificateSource = new CommonTrustedCertificateSource();
        CertificateToken certificate = DSSUtils.loadCertificate(new File("/root_cert/rootcert.pem"));
        trustedCertificateSource.addCertificate(certificate);
        certificateVerifier.setTrustedCertSources(getTrustedCertificateSource());
        validator.setCertificateVerifier(certificateVerifier);

        var asicContainerExtractor = new ASiCWithXAdESContainerExtractor(signedDocument);
        ASiCContent extractedResult = asicContainerExtractor.extract();
        extractedResult.getSignedDocuments().forEach(doc -> log.info("Extracted document: {}", doc.getName()));

        var signatures = validator.getSignatures();
        if (signatures == null || signatures.isEmpty()) {
            return null;
        }

        Reports reports = validator.validateDocument();
        DiagnosticData diagnosticData = reports.getDiagnosticData();

        diagnosticData.getSignatures().forEach(sig -> {
            if (!sig.isSignatureValid()) { return; }
            /*try {
                for(var child: XmlParserUtils.getRootChildrenExcludingSignatures(content)){
                    String name = child.getLocalName() != null ? child.getLocalName() : child.getNodeName();
                    if (dataChildHashIsValid(project, child, name)) {
                        try {
                            KnownDocuments kd = KnownDocuments.valueOf(name);
                            projectsContractStatus.registerNewSignature(
                                    project,
                                    kd,
                                    sig.getSigningCertificate().getCommonName(),
                                    sig.getSigningCertificate().getOrganizationName()
                            );
                        } catch (IllegalArgumentException ignore) {
                            log.warn("Unknown document element found in XML: {}", name);
                        }
                    }
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }*/
            log.info("Signature validity: {}", sig.isSignatureValid());
        });

        return diagnosticData.getSignatures().stream().allMatch(AbstractTokenProxy::isSignatureValid);
    }
}
