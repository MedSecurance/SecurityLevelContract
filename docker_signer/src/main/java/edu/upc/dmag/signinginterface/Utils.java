package edu.upc.dmag.signinginterface;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

public class Utils {
    public static ResponseEntity<byte[]> generateAnswer(String content, String fileName) {
        byte[] modifiedFileBytes = content.getBytes(StandardCharsets.UTF_8);

        // Zip the content
        //byte[] zipBytes = zipFile(file.getOriginalFilename(), modifiedFileBytes);

        // Set headers and return the file as an attachment
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment", fileName);

        //headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"test.xml\"");

        // Return the file in the response entity
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(modifiedFileBytes.length)
                .contentType(MediaType.APPLICATION_XML)  // Set your file's content type (PDF example)
                .body(modifiedFileBytes);
    }
}
