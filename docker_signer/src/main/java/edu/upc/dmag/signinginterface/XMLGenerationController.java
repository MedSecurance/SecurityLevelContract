package edu.upc.dmag.signinginterface;

import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.w3c.dom.*;

import software.amazon.awssdk.services.s3.model.S3Object;

@Controller
@RequiredArgsConstructor
@Slf4j
public class XMLGenerationController {
    private final MinioService minioService;

    @GetMapping("/{project}/generateUnsignedContract")
    public ResponseEntity<byte[]> generateXmlWithModel(@PathVariable String project, Model model) throws Exception {
        model.addAttribute("project", project);
        log.debug("about to request files");
        var uploaded_files = minioService.getListOfFiles(project);
        log.debug("requested files");
        uploaded_files.forEach(file -> {log.debug("file listed: {}", file);});

        Map<KnownDocuments, DownloadResult> fields = new HashMap<>();

        for(S3Object s3Object: uploaded_files) {
            log.debug("listed file: {}", s3Object.key());
            String filename_to_search = s3Object.key().replace(project + "/", "");
            log.debug("searching for file: {}", filename_to_search);
            try {
                fields.put(
                    KnownDocuments.valueOf(filename_to_search),
                    minioService.downloadAsBase64(project, s3Object)
                );
            }catch (Exception exception) {
                log.error("An error occurred while working on {}", filename_to_search, exception);
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

    @GetMapping("/{project}/generateContract")
    public ModelAndView generateContract(@PathVariable String project, Model model) throws Exception {
        model.addAttribute("project", project);
        ModelAndView mav = new ModelAndView("contract_creation_new_style.html");
        mav.addObject("documents", KnownDocuments.values());
        Map<String, S3Object> s3Objects = new HashMap<>();
        Arrays.stream(KnownDocuments.values()).forEach(d -> {
            s3Objects.put(d.name(), null);
        });
        minioService.getListOfFiles(project).forEach(it -> {
            s3Objects.put(
                KnownDocuments.valueOf(
                    it.key().replace(project+"/", "")
                ).name(), it
            );
        });
        mav.addObject("s3Objects", s3Objects);

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
