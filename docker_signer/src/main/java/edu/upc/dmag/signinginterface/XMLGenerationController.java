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
            @RequestParam(value = "Documentation_assuranceCaseReport", required = false) MultipartFile Documentation_assuranceCaseReport,
            @RequestParam(value = "Documentation_instructions+of+use", required = false) MultipartFile Documentation_instructionsOfUse,
            @RequestParam(value = "Documentation_validation+report", required = false) MultipartFile Documentation_validationReport,
            @RequestParam(value = "Documentation_technical+description", required = false) MultipartFile Documentation_technicalDescription,
            @RequestParam(value = "Documentation_Trend+Report", required = false) MultipartFile documentation_TrendReport
    ) throws Exception {
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
            extraFields.put("TVRA_AttackPaths",TVRA_AttackPaths);
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
        return fromFieldsToXML(extraFields);
    }

    static String fromFieldsToXMLBytesToString(
            Map<String,byte[]> extraFieldsToImport
    ) throws Exception {
        DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
        Document document = documentBuilder.newDocument();

        // Root element
        Element root = document.createElement("root");
        document.appendChild(root);


        for (var extraField: extraFieldsToImport.entrySet()) {
            if (extraField.getValue() == null){
                continue;
            }
            Element modelCeaDescription = document.createElement(extraField.getKey());
            modelCeaDescription.setTextContent(Base64.getEncoder().encodeToString(extraField.getValue()));
            root.appendChild(modelCeaDescription);
        }

        // Signatures
        Element signatures = document.createElement("signatures");
        root.appendChild(signatures);

        return prettyPrintXML(document);
    }

    private static ResponseEntity<byte[]> fromFieldsToXML(Map<String, MultipartFile> fields) throws Exception {
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
        return Utils.generateAnswer(fromFieldsToXMLBytesToString(castedFields), "contract.xml");
    }

    @GetMapping("/generateContract")
    public String generateXmlBySeed() throws Exception {
        return "contract_creation_new_style.html";
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
