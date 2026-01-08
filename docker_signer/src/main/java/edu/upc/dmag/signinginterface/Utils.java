package edu.upc.dmag.signinginterface;

import eu.europa.esig.dss.model.DSSDocument;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class Utils {
    private static final Logger LOGGER = Logger.getLogger(Utils.class.getName());

    public static ResponseEntity<StreamingResponseBody> generateAnswer(
            DSSDocument dssDocument,
            String fileName
    ) {



        StreamingResponseBody stream = dssDocument::writeTo;

        ContentDisposition contentDisposition = ContentDisposition.attachment().filename(fileName).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.etsi.asic-s+zip")
                .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                .body(stream);
    }

    public static ResponseEntity<StreamingResponseBody> generateAnswer(
            File tarFile,
            String fileName
    ) {

        StreamingResponseBody stream = outputStream -> {
            try (InputStream inputStream = new FileInputStream(tarFile)) {
                inputStream.transferTo(outputStream);
            } finally {
                if (tarFile != null) {
                    if(!tarFile.delete()){
                        LOGGER.warning("Could not delete tarFile " + tarFile.getAbsolutePath());
                    }
                }
            }
        };

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/x-tar"));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(fileName).build()
        );

        return ResponseEntity.ok()
                .headers(headers)
                .body(stream);
    }

    public static ResponseEntity<byte[]> generateAnswer(byte[] content, String fileName) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(content.length)
                .contentType(MediaType.APPLICATION_XML)
                .body(content);
    }

    public static ResponseEntity<byte[]> generateAnswer(String content, String fileName) {
        byte[] modifiedFileBytes = content.getBytes(StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(modifiedFileBytes.length)
                .contentType(MediaType.APPLICATION_XML)
                .body(modifiedFileBytes);
    }

    public static String sha256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (var is = Files.newInputStream(file.toPath())) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }

        return bytesToHex(digest.digest());
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }

    static File createTempFile(String prefix, String suffix, HttpServletRequest request) throws IOException {
        List<Path> tempFiles = (List<Path>) request.getAttribute("TEMP_FILES");
        if (tempFiles == null) {
            tempFiles = new ArrayList<>();
            request.setAttribute("TEMP_FILES", tempFiles);
        }
        var tmpFile = File.createTempFile(prefix, suffix);
        tempFiles.add(tmpFile.toPath());
        return tmpFile;
    }
}
