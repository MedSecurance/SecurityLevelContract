package edu.upc.dmag.signinginterface;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.*;

import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.json.JSONArray;
import org.xml.sax.SAXException;

@Controller
public class XMLGenerationController {
    static List<String> riskNames;

    static {
        try {
            riskNames = getRisksNames();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getRisksNames() throws IOException {
        String url = "http://localhost:3030/ds/sparql";
        String payload = "query=PREFIX%20foaf%3A%20%3Chttp%3A%2F%2Fxmlns.com%2Ffoaf%2F0.1%2F%3E%0APREFIX%20rdf%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%3E%0APREFIX%20rdfs%3A%20%3Chttp%3A%2F%2Fwww.w3.org%2F2000%2F01%2Frdf-schema%3E%0ASELECT%20%3Frisk%20%3FriskName%0AWHERE%20%7B%0A%20%20%3Frisk%20foaf%3Aname%20%3FriskName.%0A%20%20%3Frisk%20a%20foaf%3ARisk%20.%20%0A%7D";
        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);

        OutputStream os = connection.getOutputStream();
        os.write(payload.getBytes());
        os.flush();
        os.close();

        String response = responseToString(connection);
        return extractRisks(response);
    }

    private static List<String> extractRisks(String response) {
        List<String> risks = new ArrayList<>();
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray bindings = jsonResponse.getJSONObject("results").getJSONArray("bindings");

        for (int i = 0; i < bindings.length(); i++) {
            JSONObject entry = bindings.getJSONObject(i);
            risks.add(entry.getJSONObject("riskName").getString("value"));
        }
        return risks;
    }

    private static String responseToString(HttpURLConnection connection) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuilder response = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return response.toString();
    }

    public static List<String> generateSubSelectionRisks(int seed, List<String> riskNames) {
        return generateSubSelectionRisks(seed, riskNames, 0.1);
    }

    public static List<String> generateSubSelectionRisks(int seed, List<String> riskNames, double p) {
        Random random = new Random(seed);
        List<String> selectedRisks = new ArrayList<>();

        for (String riskName : riskNames) {
            if (random.nextDouble() < p) {
                selectedRisks.add(riskName);
            }
        }

        return selectedRisks;
    }

    @GetMapping("/generateRisks")
    public ResponseEntity<byte[]> generateXml() throws Exception {
        Random random = new Random(System.currentTimeMillis());
        int seed = random.nextInt();

        List<String> riskNames = generateSubSelectionRisks(seed, XMLGenerationController.riskNames);

        return fromRisksToXML(seed, riskNames);
    }

    @PostMapping("/generateRisksWithModel")
    public ResponseEntity<byte[]> generateXmlWithModel(@RequestParam("ceafile") MultipartFile model) throws Exception {
        Random random = new Random(System.currentTimeMillis());
        int seed = random.nextInt();

        List<String> riskNames = generateSubSelectionRisks(seed, XMLGenerationController.riskNames);

        return fromRisksToXML(seed, riskNames, model);
    }

    @PostMapping("/generateRisksWithModel")
    public ResponseEntity<byte[]> generateXmlWithModelAndEvidences(
            @RequestParam("ceafile") MultipartFile model,
            @RequestParam(value="softwareUpdatesLog", required=false) MultipartFile softwareUpdatesLog,
            @RequestParam(value="softwareVersionNumbersLog", required=false) MultipartFile softwareVersionNumbersLog,
            @RequestParam(value="validationReport", required=false) MultipartFile validationReport,
            @RequestParam(value="trendReport", required=false) MultipartFile trendReport,
            @RequestParam(value="technicalDescription", required=false) MultipartFile technicalDescription,
            @RequestParam(value="documentationIntegratedToDevice", required=false) MultipartFile documentationIntegratedToDevice,
            @RequestParam(value="riskManagementPlan", required=false) MultipartFile riskManagementPlan,
            @RequestParam(value="instructionsOfUse", required=false) MultipartFile instructionsOfUse,
            @RequestParam(value="instructionsForUse", required=false) MultipartFile instructionsForUse,
            @RequestParam(value="connectivityTroubleshootingInformationDocument", required=false) MultipartFile connectivityTroubleshootingInformationDocument,
            @RequestParam(value="disclaimerAndWarningDocument", required=false) MultipartFile disclaimerAndWarningDocument,
            @RequestParam(value="regionalAccommodationRequirement", required=false) MultipartFile regionalAccommodationRequirement,
            @RequestParam(value="documentOnUsageOfAIAndMLInDevice", required=false) MultipartFile documentOnUsageOfAIAndMLInDevice,
            @RequestParam(value="documentsOnComplianceWithJurisdictionalRegulatoryRequirements", required=false) MultipartFile documentsOnComplianceWithJurisdictionalRegulatoryRequirements,
            @RequestParam(value="regulatoryDocumentation", required=false) MultipartFile regulatoryDocumentation,
            @RequestParam(value="riskManagementFile", required=false) MultipartFile riskManagementFile,
            @RequestParam(value="deviceRecord", required=false) MultipartFile deviceRecord,
            @RequestParam(value="healthSoftwareProductIdentifierDocument", required=false) MultipartFile healthSoftwareProductIdentifierDocument,
            @RequestParam(value="documentOnManufacturerContractInformation", required=false) MultipartFile documentOnManufacturerContractInformation,
            @RequestParam(value="technicalUseSpecification", required=false) MultipartFile technicalUseSpecification,
            @RequestParam(value="trendReportings", required=false) MultipartFile trendReportings,
            @RequestParam(value="documentOnSpecialSkillsRequiredFromUser", required=false) MultipartFile documentOnSpecialSkillsRequiredFromUser,
            @RequestParam(value="instructionForUse", required=false) MultipartFile instructionForUse,
            @RequestParam(value="medicalItNetworkRiskManagementFile", required=false) MultipartFile medicalItNetworkRiskManagementFile,
            @RequestParam(value="assuranceCaseReport", required=false) MultipartFile assuranceCaseReport
    ) throws Exception {
        Random random = new Random(System.currentTimeMillis());
        int seed = random.nextInt();

        List<String> riskNames = generateSubSelectionRisks(seed, XMLGenerationController.riskNames);

        return fromRisksToXML(seed, riskNames, model);
    }

    private static Node extractRootAndAdaptIt(byte[] xmlDocumentBytes, Document documentToAdaptTo) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

        // Create a DocumentBuilder
        DocumentBuilder builder = factory.newDocumentBuilder();

        // Parse the XML file
        Document document = builder.parse(new ByteArrayInputStream(xmlDocumentBytes));

        // Normalize the XML structure
        document.getDocumentElement().normalize();

        // Get the root element
        Element root = document.getDocumentElement();
        return documentToAdaptTo.adoptNode(root);
    }

    static String fromRisksToXMLBytesToString(int seed, List<String> riskNames, byte[] toImport) throws Exception {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();

        // Root element
        Element root = document.createElement("root");
        document.appendChild(root);

        // Model description
        Element modelDescription = document.createElement("model_description");
        Text seedText = document.createTextNode(String.valueOf(seed));
        modelDescription.appendChild(seedText);
        root.appendChild(modelDescription);

        if (toImport != null) {
            Element modelCeaDescription = document.createElement("model_cea_description");
            Node rootToImport = extractRootAndAdaptIt(toImport, document);
            rootToImport = document.importNode(rootToImport, true);

            var attributes = rootToImport.getAttributes();
            for (int i = 0; i < attributes.getLength(); i++) {
                modelCeaDescription.setAttribute(attributes.item(i).getNodeName(), attributes.item(i).getNodeValue());
            }
            NodeList nodesToCopy = rootToImport.getChildNodes();
            for (int i = 0; i < nodesToCopy.getLength(); i++) {
                modelCeaDescription.appendChild(document.importNode(nodesToCopy.item(i), true));
            }
            root.appendChild(modelCeaDescription);
        }

        // Risks
        Element risks = document.createElement("risks");
        root.appendChild(risks);

        for (String riskName : riskNames) {
            Element riskElement = document.createElement("risk");
            Text riskNameText = document.createTextNode(riskName);
            riskElement.appendChild(riskNameText);
            risks.appendChild(riskElement);
        }

        // Signatures
        Element signatures = document.createElement("signatures");
        root.appendChild(signatures);


        return prettyPrintXML(document);
    }

    private static ResponseEntity<byte[]> fromRisksToXML(int seed, List<String> riskNames, MultipartFile model) throws Exception {
        return Utils.generateAnswer(fromRisksToXMLBytesToString(seed, riskNames, model.getBytes()), "risks.xml");
    }

    private static ResponseEntity<byte[]> fromRisksToXML(int seed, List<String> riskNames) throws Exception {
        return fromRisksToXML(seed, riskNames, null);
    }

    @GetMapping("/generateRisks/{seed}")
    public ResponseEntity<byte[]> generateXmlBySeed(@PathVariable int seed) throws Exception {
        List<String> riskNames = generateSubSelectionRisks(seed, XMLGenerationController.riskNames);

        return fromRisksToXML(seed, riskNames);
    }

    public static String prettyPrintXML(Document document) throws Exception {
        // Convert Document to pretty printed XML String
        java.io.StringWriter stringWriter = new java.io.StringWriter();
        javax.xml.transform.TransformerFactory transformerFactory = javax.xml.transform.TransformerFactory.newInstance();
        javax.xml.transform.Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        javax.xml.transform.dom.DOMSource domSource = new javax.xml.transform.dom.DOMSource(document);
        javax.xml.transform.stream.StreamResult streamResult = new javax.xml.transform.stream.StreamResult(stringWriter);
        transformer.transform(domSource, streamResult);
        return stringWriter.toString();
    }
}
