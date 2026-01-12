package edu.upc.dmag.signinginterface;

import eu.europa.esig.dss.asic.common.ASiCContent;
import eu.europa.esig.dss.asic.common.merge.ASiCContainerMerger;
import eu.europa.esig.dss.asic.common.merge.DefaultContainerMerger;
import eu.europa.esig.dss.asic.xades.ASiCWithXAdESContainerExtractor;
import eu.europa.esig.dss.model.DSSDocument;
import eu.europa.esig.dss.model.FileDocument;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.xml.sax.SAXException;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;


import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.security.cert.CertificateException;

@Controller
@RequestMapping("/{project}/signer")
@RequiredArgsConstructor
@Slf4j
public class SignerController {
    private final Signer signer;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
        @PathVariable String project,
        @RequestParam("to_sign") MultipartFile file,
        Model model,
        HttpServletRequest request
    ) {
        log.debug("received signing request for project '{}'", project);
        model.addAttribute("project", project);
        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            log.debug("attempting to sign as TAR");
            return signInputTarFile(project, file, request);
        } catch (Exception e) {
            try {
                log.debug("failed to sign as TAR, trying ASiC-S", e);
                return signInputAsicFile(project, file, request);
            } catch (Exception ex) {
                log.error("failed to sign document", ex);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    private ResponseEntity<StreamingResponseBody> signInputTarFile(String project, MultipartFile file, HttpServletRequest request) throws Exception {
        log.debug("extracting files from TAR");
        var providedFiles = extractFromTARFile(file, request);
        log.debug("files extracted from TAR: {}", providedFiles.keySet());

        log.debug("about to sign document");
        DSSDocument signedContent = signer.sign(project, providedFiles);
        log.debug("document is signed");

        String originalFileName = file.getOriginalFilename();
        originalFileName= originalFileName.substring(0, originalFileName.lastIndexOf('.'));
        log.info("User '{}' signed document '{}'", retrieveUsernameFromSecurityContext(), file.getOriginalFilename());
        return Utils.generateAnswer(signedContent, "signed_" + originalFileName + ".asics");
    }

    private ResponseEntity<StreamingResponseBody> signInputAsicFile(String project, MultipartFile file, HttpServletRequest request) throws Exception {
        log.debug("signing ASiC-S document directly");
        File tmpFile = Utils.createTempFile("upload-", ".tmp",request);
        file.transferTo(tmpFile);


        DSSDocument uploadedDssDocument = new FileDocument(tmpFile);
        var asicContainerExtractor = new ASiCWithXAdESContainerExtractor(uploadedDssDocument);
        ASiCContent extractedResult = asicContainerExtractor.extract();

        var content = new HashMap<KnownDocuments, File>();
        for(DSSDocument signedContent: extractedResult.getSignedDocuments()){
            File tempFile = Utils.createTempFile("extracted-", ".tmp", request);
            signedContent.writeTo(new FileOutputStream(tempFile));
            content.put(KnownDocuments.valueOf(signedContent.getName()), tempFile);
        }

        DSSDocument newlySigned = signer.sign(project, content);
        var newSignatureFile = Utils.createTempFile("signed-", ".tmp", request);
        try (FileOutputStream fos = new FileOutputStream(newSignatureFile)) {
            newlySigned.writeTo(fos);
        }
        var mergedFile = Utils.createTempFile("merged-", ".tmp", request);

        MyMerger.merge(tmpFile, newSignatureFile, mergedFile);



        extractedResult.getSignedDocuments().forEach(doc -> log.info("Extracted document: {}", doc.getName()));

        log.info("User '{}' signed document '{}'", retrieveUsernameFromSecurityContext(), file.getOriginalFilename());
        String originalFileName = file.getOriginalFilename();
        return Utils.generateASICAnswer(mergedFile, "signed_" + originalFileName);
    }

    private static Map<KnownDocuments, File> extractFromTARFile(
            MultipartFile file,
            HttpServletRequest request
    ) throws IOException {
        Map<KnownDocuments, File> tempFiles = new HashMap<>();
        try (InputStream is = file.getInputStream();
             TarArchiveInputStream tarInput = new TarArchiveInputStream(is)) {
            TarArchiveEntry currentEntry;
            while ((currentEntry = tarInput.getNextEntry()) != null) {
                if (currentEntry.isDirectory()) {
                    continue;
                }
                File tempFile = Utils.createTempFile("upload-", ".tmp", request);
                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    tarInput.transferTo(fos);
                }
                tempFiles.put(
                        KnownDocuments.valueOf(currentEntry.getName()),
                        tempFile
                );
            }
        }

        for (Map.Entry<KnownDocuments, File> entry : tempFiles.entrySet()) {
            log.info("Extracted file '{}' to temporary file '{}'", entry.getKey(), entry.getValue().getAbsolutePath());
        }
        return tempFiles;
    }

    private String retrieveUsernameFromSecurityContext() {
        return ((DefaultOidcUser)SecurityContextHolder.getContext().getAuthentication().getPrincipal()).getUserInfo().getClaim("preferred_username");
    }
}
