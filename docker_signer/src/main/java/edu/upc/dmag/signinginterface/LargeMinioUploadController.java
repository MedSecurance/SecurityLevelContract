package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;

@Slf4j
@RestController
@RequiredArgsConstructor
public class LargeMinioUploadController {
    private final ProjectsContractStatus projectsContractStatus;
    private final MinioService uploadService;

    @Value("${minio.url}")
    private String minioUrl;

    @Value("${minio.bucket.name}")
    private String bucketName;

    @PostMapping("/{project}/upload/{filename}")
    public ResponseEntity<String> uploadLargeFile(
            @PathVariable String project,
            @PathVariable String filename,
            @RequestParam("file") MultipartFile file,
            Model model) throws Exception {
        log.debug("Uploading large file: project={}, filename={}", project, filename);

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

        KnownDocuments fileToUpload = KnownDocuments.valueOf(filename);
        if (fileToUpload == KnownDocuments.ORIGINAL_NAMES) {
            return ResponseEntity.badRequest().body("Uploading files with ORIGINAL_NAMES is not allowed.");
        }

        projectsContractStatus.registerNewDocumentVersion(
            project,
            file.getOriginalFilename(),
            fileToUpload,
            s3Object,
            sha256
        );

        log.debug("Finished uploading large file: project={}, filename={}", project, filename);

        return ResponseEntity.ok(minioUrl+"/"+bucketName+"/"+key);
    }
}

