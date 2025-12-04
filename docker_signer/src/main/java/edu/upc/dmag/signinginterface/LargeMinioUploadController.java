package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class LargeMinioUploadController {
    private final ProjectsContractStatus projectsContractStatus;
    private final MinioService uploadService;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @PostMapping("/{project}/large/{filename}")
    public ResponseEntity<String> uploadLargeFile(
            @PathVariable String project,
            @PathVariable String filename,
            @RequestParam("file") MultipartFile file,
            Model model) throws Exception {
        project = project.strip();
        model.addAttribute("project", project);
        filename = filename.strip();
        String key;
        if (project.isEmpty()){
            key = filename;
        } else {
            key = project + "/" + filename;
        }

        String sha256;
        try (InputStream is = file.getInputStream()) {
            sha256 = DigestUtils.sha256Hex(is);
        }

        S3Object s3Object;
        try (InputStream is = file.getInputStream()) {
            s3Object = uploadService.uploadLargeFile(is, key);
        }

        projectsContractStatus.registerNewDocumentVersion(
            project,
            KnownDocuments.valueOf(filename),
            s3Object,
            sha256
        );

        return ResponseEntity.ok(minioUrl+"/"+bucketName+"/"+key);
    }
}

