package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;

@RestController
@RequestMapping("/upload")
@RequiredArgsConstructor
public class LargeMinioUploadController {
    private final ProjectsContractStatus projectsContractStatus;
    private final MinioService uploadService;

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

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        InputStream is = file.getInputStream();
        DigestInputStream dis = new DigestInputStream(is, md);

        S3Object s3Object = uploadService.uploadLargeFile(dis, key);

        byte[] digest = md.digest();
        String sha256 = DigestUtils.sha256Hex(digest);

        final String bucketName = "test";

        projectsContractStatus.registerNewDocumentVersion(
            project,
            KnownDocuments.valueOf(filename),
            s3Object,
            sha256
        );

        return ResponseEntity.ok("http://signer.minio/"+bucketName+"/"+key);
    }
}

