package edu.upc.dmag.signinginterface;

import java.io.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.*;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.json.JSONArray;
import org.xml.sax.SAXException;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;
import software.amazon.awssdk.services.s3.model.S3Object;

@Controller
@RequiredArgsConstructor
@Slf4j
public class XMLGenerationController {
    private final MinioService minioService;

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

    @GetMapping("/generateUnsignedContract")
    public ResponseEntity<byte[]> generateXmlWithModel() throws Exception {
        String project = "test";
        log.error("about to request files");
        var uploaded_files = minioService.getListOfFiles(project);
        log.error("requested files");
        uploaded_files.forEach(file -> {log.error("file listed: {}", file);});

        Map<KnownDocuments, DownloadResult> fields = new HashMap<>();

        for(S3Object s3Object: uploaded_files) {
            log.error("listed file: {}", s3Object.key());
            String filename_to_search = s3Object.key().replace(project + "/", "");
            log.error("searching for file: {}", filename_to_search);
            try {
                fields.put(
                    KnownDocuments.valueOf(filename_to_search),
                    minioService.downloadAsBase64(project, s3Object)
                );
            }catch (Exception ignore) {
                log.error("An error occurred while working on "+filename_to_search, ignore);
            }
        }


        return fromFieldsToXML(fields);
    }

    static String fromFieldsToXMLBytesToString(
            Map<KnownDocuments,DownloadResult> extraFieldsToImport
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
            Element documentElement = document.createElement(extraField.getKey().name());
            documentElement.appendChild(
                document.createElement("hash")).setTextContent(extraField.getValue().sha256Hash()
            );
            documentElement.appendChild(
                document.createElement("version")).setTextContent(extraField.getValue().versionId()
            );
            documentElement.appendChild(
                document.createElement("data")).setTextContent(extraField.getValue().base64Data()
            );
            root.appendChild(documentElement);
        }

        // Signatures
        Element signatures = document.createElement("signatures");
        root.appendChild(signatures);

        return prettyPrintXML(document);
    }

    private static ResponseEntity<byte[]> fromFieldsToXML(Map<KnownDocuments,DownloadResult> extraFieldsToImport) throws Exception {
        return Utils.generateAnswer(fromFieldsToXMLBytesToString(extraFieldsToImport), "contract.xml");
    }

    @GetMapping("/generateContract")
    public ModelAndView generateXmlBySeed() throws Exception {
        ModelAndView mav = new ModelAndView("contract_creation_new_style.html");
        mav.addObject("documents", KnownDocuments.values());

        return mav;
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
