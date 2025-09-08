package edu.upc.dmag.signinginterface;

import java.io.*;
import java.util.*;
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
import java.util.stream.Collectors;

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
    public ResponseEntity<byte[]> generateXmlWithModel(
            //For modelling
            @RequestParam("ceafile") MultipartFile model,
            //For IoMT recommendation
            @RequestParam(value = "CR_model", required = false) MultipartFile CR_model,
            @RequestParam(value = "CR_result", required = false) MultipartFile CR_result,
            //For TVRA
            @RequestParam(value = "TVRA_Model", required = false) MultipartFile TVRA_model,
            @RequestParam(value = "TVRA_Attack+Paths", required = false) MultipartFile TVRA_AttackPaths,
            @RequestParam(value = "TVRA_Recommendations", required = false) MultipartFile TVRA_Recommendations,
            @RequestParam(value = "TVRA_Threats", required = false) MultipartFile TVRA_Threats,
            @RequestParam(value = "TVRA_Report", required = false) MultipartFile TVRA_Report,
            //For assurance
            @RequestParam(value = "Documentation_risk+management+plan", required = false) MultipartFile Documentation_riskManagementPlan,
            @RequestParam(value = "GeneralContent_security+case", required = false) MultipartFile GeneralContent_securityCase,
            @RequestParam(value = "Documentation_medical+it-network+risk+management+file", required = false) MultipartFile Documentation_medicalItNetworkRiskManagementFile,
            @RequestParam(value = "Documentation_assuranceCaseReport", required = false) MultipartFile Documentation_assuranceCaseReport
            @RequestParam(value = "Documentation_instructions+of+use", required = false) MultipartFile Documentation_instructionsOfUse,
            @RequestParam(value = "Documentation_validation+report", required = false) MultipartFile Documentation_validationReport,
            @RequestParam(value = "Documentation_technical+description", required = false) MultipartFile Documentation_technicalDescription,
            @RequestParam(value = "Documentation_Trend+Report", required = false) MultipartFile documentation_TrendReport,
            @RequestParam(value = "Documentation_software+updates+log", required = false) MultipartFile Documentation_softwareUpdatesLog,
            @RequestParam(value = "Documentation_software+version+numbers+log", required = false) MultipartFile Documentation_softwareVersionNumbersLog,
            @RequestParam(value = "Documentation_regulatory+documentation", required = false) MultipartFile Documentation_regulatoryDocumentation,
            @RequestParam(value = "Documentation_device+record", required = false) MultipartFile Documentation_deviceRecord,
            @RequestParam(value = "Documentation_Technical+Use+Specification", required = false) MultipartFile Documentation_technicalUseSpecification,
    ) throws Exception {
        Random random = new Random(System.currentTimeMillis());
        int seed = random.nextInt();

        List<String> riskNames = generateSubSelectionRisks(seed, XMLGenerationController.riskNames);
        Map<String, MultipartFile> extraFields = new HashMap<>();
        extraFields.put("ceafile", model);

        if (CR_model != null){
            extraFields.put("CR_model",CR_model);
        }
        if (CR_result != null){
            extraFields.put( "CR_result",CR_result);
        }


        if (TVRA_model != null){
            extraFields.put("TVRA_Model",TVRA_model);
        }
        if (TVRA_AttackPaths != null){
            extraFields.put("TVRA_Attack+Paths",TVRA_AttackPaths);
        }
        if (TVRA_Recommendations != null){
            extraFields.put( "TVRA_Recommendations",TVRA_Recommendations);
        }
        if (TVRA_Threats != null){
            extraFields.put("TVRA_Threats",TVRA_Threats);
        }
        if (TVRA_Report != null){
            extraFields.put("TVRA_Report",TVRA_Report);
        }
        if (Documentation_riskManagementPlan != null){
            extraFields.put("Documentation_riskManagementPlan", Documentation_riskManagementPlan);;
        }
        if (GeneralContent_securityCase != null){
            extraFields.put("GeneralContent_securityCase", GeneralContent_securityCase);
        }
        if (Documentation_medicalItNetworkRiskManagementFile != null){
            extraFields.put("Documentation_medicalItNetworkRiskManagementFile", Documentation_medicalItNetworkRiskManagementFile);;
        }
        if (Documentation_assuranceCaseReport != null){
            extraFields.put("Documentation_assuranceCaseReport", Documentation_assuranceCaseReport);;
        }
        if (Documentation_instructionsOfUse != null){
            extraFields.put("Documentation_instructionsOfUse", Documentation_instructionsOfUse);;
        }
        if (Documentation_validationReport != null){
            extraFields.put("Documentation_validationReport", Documentation_validationReport);;
        }
        if (Documentation_technicalDescription != null){
            extraFields.put("Documentation_technicalDescription", Documentation_technicalDescription);;
        }
        if (documentation_TrendReport != null){
            extraFields.put("Documentation_TrendReport", documentation_TrendReport);;
        }
        if (Documentation_softwareUpdatesLog != null){
            extraFields.put("Documentation_softwareUpdatesLog", Documentation_softwareUpdatesLog);;
        }
        if (Documentation_softwareVersionNumbersLog != null){
            extraFields.put("Documentation_softwareVersionNumbersLog", Documentation_softwareVersionNumbersLog);;
        }
        if (Documentation_regulatoryDocumentation != null){
            extraFields.put("Documentation_regulatoryDocumentation", Documentation_regulatoryDocumentation);;
        }
        if (Documentation_deviceRecord != null){
            extraFields.put("Documentation_deviceRecord", Documentation_deviceRecord);;
        }
        if (Documentation_technicalUseSpecification != null){
            extraFields.put("Documentation_technicalUseSpecification", Documentation_technicalUseSpecification);;
        }
        return fromRisksToXML(seed, riskNames, extraFields);
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

    static String fromRisksToXMLBytesToString(int seed, List<String> riskNames, Map<String,byte[]> extraFieldsToImport) throws Exception {
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

        for (var extraField: extraFieldsToImport.entrySet()) {
            if (extraField.getValue() == null){
                continue;
            }
            Element modelCeaDescription = document.createElement(extraField.getKey());
            modelCeaDescription.setTextContent(Base64.getEncoder().encodeToString(extraField.getValue()));
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

    private static ResponseEntity<byte[]> fromRisksToXML(int seed, List<String> riskNames, Map<String, MultipartFile> fields) throws Exception {
        var castedFields = fields.entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    try {
                        return entry.getValue().getBytes();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
        ));
        return Utils.generateAnswer(fromRisksToXMLBytesToString(seed, riskNames, castedFields), "risks.xml");
    }

    private static ResponseEntity<byte[]> fromRisksToXML(int seed, List<String> riskNames) throws Exception {
        return fromRisksToXML(seed, riskNames, Map.of());
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
