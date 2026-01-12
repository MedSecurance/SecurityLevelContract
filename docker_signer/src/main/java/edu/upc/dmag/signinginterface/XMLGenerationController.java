package edu.upc.dmag.signinginterface;

import java.io.*;
import java.util.*;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import  org.apache.commons.io.IOUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.w3c.dom.*;

import software.amazon.awssdk.services.s3.model.S3Object;

@Controller
@RequiredArgsConstructor
@Slf4j
public class XMLGenerationController {
    private final MinioService minioService;

    @GetMapping("/{project}/generateUnsignedContract")
    public ResponseEntity<StreamingResponseBody> generateXmlWithModel(
            @PathVariable String project,
            Model model,
            HttpServletRequest request
    ) throws Exception {
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
                    minioService.download(project, s3Object, request)
                );
            }catch (Exception exception) {
                log.error("An error occurred while working on {}", filename_to_search, exception);
            }
        }

        File tmpTarFile = Utils.createTempFile("contract_", ".tar", request);
        return fromFieldsToTar(tmpTarFile, fields);
    }

    private static ResponseEntity<StreamingResponseBody> fromFieldsToTar(
            File tmpTarFile,
            Map<KnownDocuments,DownloadResult> extraFieldsToImport
    ) throws Exception {
        fromFieldsToTarToSign(tmpTarFile, extraFieldsToImport);
        return Utils.generateTarAnswer(tmpTarFile, "contract.tar");
    }

    private static void fromFieldsToTarToSign(File tarFile, Map<KnownDocuments, DownloadResult> extraFieldsToImport) {

        try (FileOutputStream fos = new FileOutputStream(tarFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             TarArchiveOutputStream tarOut = new TarArchiveOutputStream(bos)) {

            // Recommended for POSIX compatibility
            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);

            for (Map.Entry<KnownDocuments, DownloadResult> entry : extraFieldsToImport.entrySet()) {
                KnownDocuments entryName = entry.getKey();
                File file = entry.getValue().file();

                TarArchiveEntry tarEntry = new TarArchiveEntry(file, entryName.name());
                tarOut.putArchiveEntry(tarEntry);

                try (FileInputStream fis = new FileInputStream(file)) {
                    IOUtils.copy(fis, tarOut);
                }

                tarOut.closeArchiveEntry();
            }

            tarOut.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
