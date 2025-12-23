package edu.upc.dmag.signinginterface;

import eu.europa.esig.dss.model.DSSDocument;
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
        Model model
    ) {
        model.addAttribute("project", project);
        if (file.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        try {
            log.debug("extracting files from TAR");
            var providedFiles = extractFromTARFile(file);
            log.debug("files extracted from TAR: {}", providedFiles.keySet());

            log.debug("about to sign document");
            DSSDocument signedContent = signer.sign(project, providedFiles);
            log.debug("document is signed");

            log.info("User '{}' signed document '{}'", retrieveUsernameFromSecurityContext(), file.getOriginalFilename());
            return Utils.generateAnswer(signedContent, "signed_" + file.getOriginalFilename());

        } catch (IOException | ParserConfigurationException | SAXException | TransformerException | XMLStreamException |
                 CertificateException e) {
            log.error("failed to sign document", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<KnownDocuments, File> extractFromTARFile(MultipartFile file) throws IOException {
        Map<KnownDocuments, File> tempFiles = new HashMap<>();
        try (InputStream is = file.getInputStream();
             TarArchiveInputStream tarInput = new TarArchiveInputStream(is)) {
            TarArchiveEntry currentEntry;
            while ((currentEntry = tarInput.getNextEntry()) != null) {
                if (currentEntry.isDirectory()) {
                    continue;
                }
                File tempFile = Files.createTempFile("upload-", ".tmp").toFile();
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
