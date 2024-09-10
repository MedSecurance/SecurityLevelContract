package edu.upc.dmag;

import eu.europa.esig.dss.diagnostic.DiagnosticData;
import eu.europa.esig.dss.diagnostic.SignatureWrapper;
import eu.europa.esig.dss.diagnostic.TimestampWrapper;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.SignatureLevel;
import eu.europa.esig.dss.enumerations.SignaturePackaging;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import eu.europa.esig.dss.model.SignatureValue;
import eu.europa.esig.dss.model.ToBeSigned;
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
import eu.europa.esig.dss.token.AbstractKeyStoreTokenConnection;
import eu.europa.esig.dss.token.DSSPrivateKeyEntry;
import eu.europa.esig.dss.token.KeyStoreSignatureTokenConnection;
import eu.europa.esig.dss.token.SignatureTokenConnection;
import eu.europa.esig.dss.utils.Utils;
import eu.europa.esig.dss.validation.CertificateVerifier;
import eu.europa.esig.dss.validation.CommonCertificateVerifier;
import eu.europa.esig.dss.validation.DocumentValidatorFactory;
import eu.europa.esig.dss.validation.SignedDocumentValidator;
import eu.europa.esig.dss.validation.reports.Reports;
import eu.europa.esig.dss.xades.XAdESSignatureParameters;
import eu.europa.esig.dss.xades.signature.XAdESService;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Date;
import java.util.List;
import java.util.ServiceLoader;

public class Main {
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

    protected static CertificateSource getModifiedOnlineTrustedCertificateSource() throws IOException {
        byte[] trustedStoreContent = getOnlineKeystoreContent("trust-anchors.jks");
        KeyStoreCertificateSource keystore = new KeyStoreCertificateSource(new File("src/main/resources/trust-anchors.jks"), "JKS", PKI_FACTORY_KEYSTORE_PASSWORD);
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

    protected static SignatureTokenConnection getUserPkcs12Token() throws IOException {
        return getOnlinePKCS12Token();
    }

    protected static String getSigningAlias() {
        return GOOD_USER;
    }

    protected static String getPKCS12KeystoreName() {
        return DSSUtils.encodeURI(getSigningAlias() + ".p12");
    }

    protected static AbstractKeyStoreTokenConnection getOnlinePKCS12Token() {
        return new KeyStoreSignatureTokenConnection(getOnlineKeystoreContent(getPKCS12KeystoreName()), "PKCS12",
                new KeyStore.PasswordProtection(PKI_FACTORY_KEYSTORE_PASSWORD));
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



    protected static TSPSource getOnlineTSPSource() {
        return getOnlineTSPSourceByName(GOOD_TSA);
    }

    public static void main(String[] args) throws IOException {
        DSSDocument toSignDocument = new FileDocument(new File("/data/to_sign.xml"));

        List<String> pathToKeys = List.of(new String[]{
                "src/main/resources/consumer.p12",
                "src/main/resources/provider.p12"
        });
        List<char[]> keysForCertificate = List.of(new char[][]{
                {'c', 'o', 'n', 's', 'u', 'm', 'e', 'r', '_', 'k', 'e', 'y'},
                {'p', 'r', 'o', 'v', 'i', 'd', 'e', 'r', '_', 'k', 'e', 'y'},
        });

        for (int i=0; i< pathToKeys.size(); i++){
            String pathToKey = pathToKeys.get(i);
            char[] keyForCertificate = keysForCertificate.get(i);
            DSSDocument signedDocument = basicSignDocument(
                    toSignDocument,
                    pathToKey,
                    keyForCertificate
            );
            toSignDocument = signedDocument;
        }

        DSSDocument tLevelSignature = extendToT(toSignDocument);
        DSSDocument ltLevelDocument = extendToLT(tLevelSignature);
        DSSDocument ltaLevelDocument = extendToLTA(ltLevelDocument);




        // end::demoLTAExtend[]

        ltaLevelDocument.save("/data/signed.xml");

        ServiceLoader<DocumentValidatorFactory> serviceLoaders = ServiceLoader.load(DocumentValidatorFactory.class);
        for (DocumentValidatorFactory factory : serviceLoaders) {
            System.out.println("one factory found: "+ factory.getClass());
        }


        testFinalDocument(ltaLevelDocument);
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
            parameters.setXPathLocationString("/requirements/signatures");

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

    static void assertNotNull(Object object){
        if (object == null){
            throw new AssertionError();
        }
    }

    static void assertTrue(boolean param){
        if (!param){
            throw new AssertionError();
        }
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
        assertNotNull(reports);
        System.out.println(
            reports.getSimpleReport()
        );
        System.out.println("");
        System.out.println(
                reports.getXmlValidationReport()
        );

        DiagnosticData diagnosticData = reports.getDiagnosticData();

        List<SignatureWrapper> signatures = diagnosticData.getSignatures();
        for (SignatureWrapper signatureWrapper : signatures) {
            assertTrue(signatureWrapper.isBLevelTechnicallyValid());

            List<TimestampWrapper> timestampList = signatureWrapper.getTimestampList();
            for (TimestampWrapper timestampWrapper : timestampList) {
                assertTrue(timestampWrapper.isMessageImprintDataFound());
                assertTrue(timestampWrapper.isMessageImprintDataIntact());
                assertTrue(timestampWrapper.isSignatureValid());
            }
        }

        return diagnosticData;
    }
}