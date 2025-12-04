package edu.upc.dmag.signinginterface;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.nio.charset.StandardCharsets;

public class Utils {
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
}
