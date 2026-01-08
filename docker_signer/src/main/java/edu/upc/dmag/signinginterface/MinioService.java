package edu.upc.dmag.signinginterface;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class MinioService {

    private final S3AsyncClient s3;

    @Value("${minio.bucket.name}")
    private String bucketName;


    private static final int PART_SIZE = 10 * 1024 * 1024; // 10 MB

    //ToDo simplify this code
    public S3Object uploadLargeFile(InputStream input, String key) throws Exception {

        CreateMultipartUploadRequest createReq = CreateMultipartUploadRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        String uploadId = s3.createMultipartUpload(createReq)
                .get()
                .uploadId();

        List<CompletedPart> completedParts = new ArrayList<>();
        byte[] buffer = new byte[PART_SIZE];
        int bytesRead;
        int partNumber = 1;

        try {
            while ((bytesRead = input.read(buffer)) > 0) {

                byte[] partData = (bytesRead == PART_SIZE)
                        ? buffer
                        : java.util.Arrays.copyOf(buffer, bytesRead);

                UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .uploadId(uploadId)
                        .partNumber(partNumber)
                        .contentLength((long) bytesRead)
                        .build();

                CompletableFuture<UploadPartResponse> future =
                        s3.uploadPart(uploadPartRequest, AsyncRequestBody.fromBytes(partData));

                UploadPartResponse response = future.get();

                completedParts.add(
                        CompletedPart.builder()
                                .partNumber(partNumber)
                                .eTag(response.eTag())
                                .build()
                );

                partNumber++;
            }

            CompleteMultipartUploadRequest completeReq =
                    CompleteMultipartUploadRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .uploadId(uploadId)
                            .multipartUpload(
                                    CompletedMultipartUpload.builder()
                                            .parts(completedParts)
                                            .build()
                            )
                            .build();

            s3.completeMultipartUpload(completeReq).get();
            ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix(key)
                    .maxKeys(1)
                    .build();

            ListObjectsV2Response listRes = s3.listObjectsV2(listReq).get();

            if (listRes.contents().isEmpty()) {
                throw new IllegalStateException("Object not found after upload: " + key);
            }

            return listRes.contents().getFirst();

        } catch (Exception ex) {

            s3.abortMultipartUpload(AbortMultipartUploadRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .uploadId(uploadId)
                    .build()).get();

            throw ex;
        }
    }

    public List<S3Object> getListOfFiles(String project) throws ExecutionException, InterruptedException {
        // List objects in the folder
        ListObjectsV2Request listReq = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(project)
                .build();

        CompletableFuture<ListObjectsV2Response> intermediary = s3.listObjectsV2(listReq);
        ListObjectsV2Response listRes = intermediary.get();
        return listRes.contents();
    }

    private String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);

            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute hash", e);
        }
    }

    private String sha256(File file) throws IOException, NoSuchAlgorithmException {
        return Utils.sha256(file);
    }





    public DownloadResult download(String bucket, S3Object obj, HttpServletRequest request) throws ExecutionException, InterruptedException, IOException {
        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(obj.key())
                .build();

        return getDownloadResult(req, request);
    }

    private DownloadResult getDownloadResult(GetObjectRequest req, HttpServletRequest request) throws InterruptedException, ExecutionException, IOException {
        File temoraryFile = Utils.createTempFile("s3object-", ".tmp", request);
        return s3.getObject(req, AsyncResponseTransformer.toFile(temoraryFile))
                .thenApply(objectResponse -> {
                    String hash = null;
                    try {
                        hash = sha256(temoraryFile);
                    } catch (IOException | NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }

                    // Version ID preferred; if not present, fall back to ETag
                    String version = objectResponse.versionId() != null
                            ?objectResponse.versionId()
                            : objectResponse.eTag();

                    return new DownloadResult(version, hash, temoraryFile);
                }).get();
    }

}

