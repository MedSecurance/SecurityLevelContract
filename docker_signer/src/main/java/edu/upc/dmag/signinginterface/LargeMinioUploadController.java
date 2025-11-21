package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class LargeMinioUploadController {

    private final MinioMultipartUploadService uploadService;

    @PostMapping("/large")
    public ResponseEntity<String> uploadLargeFile(@RequestParam("file") MultipartFile file) throws Exception {

        String key = file.getOriginalFilename();
        uploadService.uploadLargeFile(file.getInputStream(), key);

        return ResponseEntity.ok("Uploaded to MinIO: " + key);
    }
}

