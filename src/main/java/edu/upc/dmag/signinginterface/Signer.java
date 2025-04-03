package edu.upc.dmag.signinginterface;

import eu.europa.esig.dss.diagnostic.AbstractTokenProxy;
import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
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
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;
import org.w3c.dom.Document;
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
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Signer {
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

    protected static String removeSignatures(String inputXML) throws ParserConfigurationException, IOException, SAXException, TransformerException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        System.out.println(inputXML);
        ByteArrayInputStream input = new ByteArrayInputStream(inputXML.getBytes(StandardCharsets.UTF_8));
        Document document = builder.parse(input);

        // Get the <signatures> element and remove all its children
        NodeList signaturesList = document.getElementsByTagName("signatures");
        if (signaturesList.getLength() > 0) {
            Node signaturesNode = signaturesList.item(0);
            while (signaturesNode.hasChildNodes()) {
                signaturesNode.removeChild(signaturesNode.getFirstChild());
            }
        }

        // Convert the modified document back to a string in memory
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        //transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        DOMSource source = new DOMSource(document);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        StreamResult result = new StreamResult(output);
        transformer.transform(source, result);

        return output.toString(StandardCharsets.UTF_8);
    }

    protected static CertificateSource getModifiedOnlineTrustedCertificateSource() throws IOException {
        //KeyStoreCertificateSource keystore = new KeyStoreCertificateSource(new File("C:\\Users\\narow\\IdeaProjects\\SigningInterface\\docker_CA\\signing_keys\\trust_anchors\\trust-anchors.jks"), "JKS", PKI_FACTORY_KEYSTORE_PASSWORD);
        KeyStoreCertificateSource keystore = new KeyStoreCertificateSource(new File("/trust_anchors/trust-anchors.jks"), "JKS", PKI_FACTORY_KEYSTORE_PASSWORD);
        CommonTrustedCertificateSource trustedCertificateSource = new CommonTrustedCertificateSource();
        trustedCertificateSource.importAsTrusted(keystore);
        return trustedCertificateSource;
    }

    protected static CertificateSource getTrustedCertificateSource() throws IOException {
        if (trustedCertificateSource == null) {
            trustedCertificateSource = new CommonTrustedCertificateSource();
        }

        getModifiedOnlineTrustedCertificateSource().getCertificates().forEach(trustedCertificateSource::addCertificate);
        return trustedCertificateSource;
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

    protected static String test(String content) throws IOException, ParserConfigurationException, SAXException, TransformerException {
        List<String> pathToKeys = List.of(new String[]{
                //"C:\\Users\\narow\\IdeaProjects\\SigningInterface\\docker_CA\\signing_keys\\consumer\\consumer.p12",
                //"C:\\Users\\narow\\IdeaProjects\\SigningInterface\\docker_CA\\signing_keys\\provider\\provider.p12",
                "/key/key.p12"
        });
        List<char[]> keysForCertificate = List.of(new char[][]{
                {'k', 'e', 'y'}
        });
        List<String> outputPaths = new ArrayList<>();

        storeBytes(content.getBytes(), "/tmp/original.xml");
        outputPaths.add("/tmp/original.xml");

        content = removeSignatures(content);

        for (int i=0; i< pathToKeys.size(); i++){
            DSSDocument toSignDocument = new InMemoryDocument(content.getBytes());

            String pathToKey = pathToKeys.get(i);
            char[] keyForCertificate = keysForCertificate.get(i);
            DSSDocument signedDocument = basicSignDocument(
                    toSignDocument,
                    pathToKey,
                    keyForCertificate
            );
            toSignDocument = signedDocument;

            DSSDocument tLevelSignature = extendToT(toSignDocument);
            DSSDocument ltLevelDocument = extendToLT(tLevelSignature);
            DSSDocument ltaLevelDocument = extendToLTA(ltLevelDocument);

            String outputPath = "/tmp/signed_"+i+".xml";
            ltaLevelDocument.save(outputPath);
            outputPaths.add(outputPath);
        }

        var mergedResult = merge_signatures(outputPaths);
        return document_to_string(mergedResult);


        /*DSSDocument ltaLevelDocument = new FileDocument(new File("/data/signed.xml"));
        ltaLevelDocument.save("/data/signed.xml");

        ServiceLoader<DocumentValidatorFactory> serviceLoaders = ServiceLoader.load(DocumentValidatorFactory.class);
        for (DocumentValidatorFactory factory : serviceLoaders) {
            System.out.println("one factory found: "+ factory.getClass());
        }


        testFinalDocument(ltaLevelDocument);*/
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
        XAdESSignatureParameters parameters;
        XAdESService xadesService;
        CommonCertificateVerifier certificateVerifier;

        // end::demoLTExtend[]

        // tag::demoLTAExtend[]
        // import eu.europa.esig.dss.enumerations.SignatureLevel;
        // import eu.europa.esig.dss.model.DSSDocument;
        // import eu.europa.esig.dss.service.crl.OnlineCRLSource;
        // import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
        // import eu.europa.esig.dss.validation.CommonCertificateVerifier;
        // import eu.europa.esig.dss.xades.XAdESSignatureParameters;
        // import eu.europa.esig.dss.xades.signature.XAdESService;

        // Create signature parameters with target extension level
        parameters = new XAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_LTA);

        // Initialize CertificateVerifier with data revocation data requesting
        certificateVerifier = new CommonCertificateVerifier();

        // init revocation sources for CRL/OCSP requesting
        certificateVerifier.setCrlSource(new OnlineCRLSource());
        certificateVerifier.setOcspSource(new OnlineOCSPSource());

        // Trust anchors should be defined for revocation data requesting
        certificateVerifier.setTrustedCertSources(getTrustedCertificateSource());

        // Initialize signature service with TSP Source for time-stamp requesting
        xadesService = new XAdESService(certificateVerifier);
        xadesService.setTspSource(getOnlineTSPSource());

        // Extend signature
        DSSDocument ltaLevelDocument = xadesService.extendDocument(ltLevelDocument, parameters);
        return ltaLevelDocument;
    }

    private static DSSDocument extendToLT(DSSDocument tLevelSignature) throws IOException {
        XAdESService xadesService;
        XAdESSignatureParameters parameters;
        CommonCertificateVerifier certificateVerifier;

        // end::demoTExtend[]

        // tag::demoLTExtend[]
        // import eu.europa.esig.dss.enumerations.SignatureLevel;
        // import eu.europa.esig.dss.model.DSSDocument;
        // import eu.europa.esig.dss.service.crl.OnlineCRLSource;
        // import eu.europa.esig.dss.service.ocsp.OnlineOCSPSource;
        // import eu.europa.esig.dss.validation.CommonCertificateVerifier;
        // import eu.europa.esig.dss.xades.XAdESSignatureParameters;
        // import eu.europa.esig.dss.xades.signature.XAdESService;

        // Create signature parameters with target extension level
        parameters = new XAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_LT);

        // Create a CertificateVerifier with revocation sources for -LT level extension
        certificateVerifier = new CommonCertificateVerifier();

        // init revocation sources for CRL/OCSP requesting
        certificateVerifier.setCrlSource(new OnlineCRLSource());
        certificateVerifier.setOcspSource(new OnlineOCSPSource());

        // Trust anchors should be defined for revocation data requesting
        certificateVerifier.setTrustedCertSources(getTrustedCertificateSource());

        // Init service for signature augmentation
        xadesService = new XAdESService(certificateVerifier);
        xadesService.setTspSource(getOnlineTSPSource());

        // Extend signature
        DSSDocument ltLevelDocument = xadesService.extendDocument(tLevelSignature, parameters);
        return ltLevelDocument;
    }

    private static DSSDocument extendToT(DSSDocument signedDocument) {
        // tag::demoTExtend[]
        // import eu.europa.esig.dss.enumerations.SignatureLevel;
        // import eu.europa.esig.dss.model.DSSDocument;
        // import eu.europa.esig.dss.validation.CommonCertificateVerifier;
        // import eu.europa.esig.dss.xades.XAdESSignatureParameters;
        // import eu.europa.esig.dss.xades.signature.XAdESService;

        // Create signature parameters with target extension level
        XAdESSignatureParameters parameters = new XAdESSignatureParameters();
        parameters.setSignatureLevel(SignatureLevel.XAdES_BASELINE_T);

        // Create a CertificateVerifier (empty configuration is possible for T-level extension)
        CommonCertificateVerifier certificateVerifier = new CommonCertificateVerifier();

        // Init service for signature augmentation
        XAdESService xadesService = new XAdESService(certificateVerifier);

        // init TSP source for timestamp requesting
        xadesService.setTspSource(getOnlineTSPSource());

        DSSDocument tLevelSignature = xadesService.extendDocument(signedDocument, parameters);
        return tLevelSignature;
    }

    protected static SignedDocumentValidator getValidator(DSSDocument signedDocument) {
        SignedDocumentValidator validator = SignedDocumentValidator.fromDocument(signedDocument);
        validator.setCertificateVerifier(new CommonCertificateVerifier());
        return validator;
    }

    protected static DiagnosticData testFinalDocument(DSSDocument signedDocument) throws IOException {
        return testFinalDocument(signedDocument, null);
    }

    protected static DiagnosticData testFinalDocument(DSSDocument signedDocument, List<DSSDocument> detachedContents) throws IOException {
        SignedDocumentValidator validator = getValidator(signedDocument);

        CertificateVerifier certificateVerifier = new CommonCertificateVerifier();
        CommonTrustedCertificateSource  trustedCertificateSource = new CommonTrustedCertificateSource();
        CertificateToken certificate = DSSUtils.loadCertificate(new File("src/main/resources/cc-root-ca.crt"));
        trustedCertificateSource.addCertificate(certificate);
        certificateVerifier.setTrustedCertSources(getTrustedCertificateSource());
        validator.setCertificateVerifier(certificateVerifier);

        if (Utils.isCollectionNotEmpty(detachedContents)) {
            validator.setDetachedContents(detachedContents);
        }
        Reports reports = validator.validateDocument();
        return reports.getDiagnosticData();
    }

    public static boolean validate(String content) throws IOException {
        DSSDocument signedDocument = new InMemoryDocument(content.getBytes());
        SignedDocumentValidator validator = getValidator(signedDocument);

        CertificateVerifier certificateVerifier = new CommonCertificateVerifier();
        CommonTrustedCertificateSource  trustedCertificateSource = new CommonTrustedCertificateSource();
        CertificateToken certificate = DSSUtils.loadCertificate(new File("/root_cert/rootcert.pem"));
        trustedCertificateSource.addCertificate(certificate);
        certificateVerifier.setTrustedCertSources(getTrustedCertificateSource());
        validator.setCertificateVerifier(certificateVerifier);

        Reports reports = validator.validateDocument();
        DiagnosticData diagnosticData = reports.getDiagnosticData();

        return diagnosticData.getSignatures().stream().allMatch(AbstractTokenProxy::isSignatureValid);
    }
}
