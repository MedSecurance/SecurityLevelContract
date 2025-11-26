package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class LargeMinioUploadController {

    private final MinioMultipartUploadService uploadService;

    @PostMapping("/large/{project}/{filename}")
    public ResponseEntity<String> uploadLargeFile(
            @PathVariable String project,
            @PathVariable String filename,
            @RequestParam("file") MultipartFile file) throws Exception {
        project = project.strip();
        filename = filename.strip();
        String key;
        if (project.isEmpty()){
            key = filename;
        } else {
            key = project + "/" + filename;
        }

        uploadService.uploadLargeFile(file.getInputStream(), key);

        final String bucketName = "test";

        return ResponseEntity.ok("http://signer.minio/"+bucketName+"/"+key);
    }
}

