package edu.upc.dmag.signinginterface;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.endpoints.internal.Value;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class MinioMultipartUploadService {

    private final S3AsyncClient s3;
    private final String bucketName = "test";

    private static final int PART_SIZE = 10 * 1024 * 1024; // 10 MB

    public String uploadLargeFile(InputStream input, String key) throws Exception {

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
            return key;

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

        CompletableFuture<ListObjectsV2Response> itermediary = s3.listObjectsV2(listReq);
        ListObjectsV2Response listRes = itermediary.get();
        s3.close();
        return listRes.contents();
    }
}

