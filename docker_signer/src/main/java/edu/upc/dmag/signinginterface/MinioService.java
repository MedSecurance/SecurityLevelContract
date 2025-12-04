package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.security.MessageDigest;
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


    public DownloadResult downloadAsBase64(String bucket, S3Object obj) throws ExecutionException, InterruptedException {

        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(bucket)
                .key(obj.key())
                .build();

        return getDownloadResult(req);
    }

    private DownloadResult getDownloadResult(GetObjectRequest req) throws InterruptedException, ExecutionException {
        return s3.getObject(req, AsyncResponseTransformer.toBytes())
                .thenApply(responseBytes -> {
                    byte[] data = responseBytes.asByteArray();

                    String base64 = Base64.getEncoder().encodeToString(data);
                    String hash = sha256(data);

                    // Version ID preferred; if not present, fall back to ETag
                    String version = responseBytes.response().versionId() != null
                            ? responseBytes.response().versionId()
                            : responseBytes.response().eTag();

                    return new DownloadResult(version, hash, base64);
                }).get();
    }

}

