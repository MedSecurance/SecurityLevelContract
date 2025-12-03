package edu.upc.dmag.signinginterface;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Component
@Lazy(false)
@Slf4j
@RequiredArgsConstructor
public class ProjectsContractStatus {
    private final S3AsyncClient s3;
    private final Map<String, ContractStatus> projects = new HashMap<>();
    private static final String BUCKET = "test";
    private static final String KEY = "ProjectsContractStatus.json";

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    @PostConstruct
    public void init() throws IOException {
        ObjectMapper mapper = createMapper();

        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(KEY)
                .build();

        try {
            ResponseBytes<GetObjectResponse> bytes = s3.getObject(req, AsyncResponseTransformer.toBytes()).join();
            byte[] data = bytes.asByteArray();

            TypeReference<Map<String, ContractStatus>> typeRef = new TypeReference<>() {};
            Map<String, ContractStatus> fromS3 = mapper.readValue(data, typeRef);

            synchronized (projects) {
                projects.clear();
                projects.putAll(fromS3);
            }
            log.info("Loaded ProjectsContractStatus from S3/MinIO");
        } catch (RuntimeException e) {
            Throwable cause = e instanceof CompletionException ? e.getCause() : e;
            if (cause instanceof S3Exception && ((S3Exception) cause).statusCode() == 404) {
                log.info("ProjectsContractStatus object not found in S3/MinIO ({}). Starting with empty state.", KEY);
            } else {
                log.error("Failed to load ProjectsContractStatus from S3/MinIO, starting with empty state", e);
            }
        } catch (IOException e) {
            log.error("Failed to parse ProjectsContractStatus JSON from S3/MinIO, starting with empty state", e);
        }
    }





    public void registerNewDocumentVersion(String project, KnownDocuments knownDocument, S3Object s3Object) {
        var contractStatus = this.projects.computeIfAbsent(project, k -> new ContractStatus());
        var documentStatus = contractStatus.documents.computeIfAbsent(knownDocument, k -> new DocumentStatus());
        documentStatus.setTimestamp(s3Object.lastModified());
        documentStatus.setHash(s3Object.eTag());
        saveAsync();

    }

    public Set<String> getOrganizationsForProject(String project) {
        var contractStatus = this.projects.computeIfAbsent(project, k-> new ContractStatus());
        return contractStatus.getOrganizations();
    }

    public Map<KnownDocuments, DocumentStatus> getDocumentsStatusForProject(String project) {
        var contractStatus = this.projects.computeIfAbsent(project, k-> new ContractStatus());
        return contractStatus.getDocuments();
    }


    public CompletableFuture<Void> saveAsync() {
        final byte[] data;
        try {
            ObjectMapper mapper = createMapper();
            synchronized (projects) {
                // snapshot to avoid concurrent-modification during serialization
                data = mapper.writeValueAsBytes(new HashMap<>(projects));
            }
        } catch (IOException e) {
            log.error("Failed to serialize projects for saving", e);
            return CompletableFuture.failedFuture(e);
        }

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(BUCKET)
                .key(KEY)
                .contentType("application/json")
                .contentLength((long) data.length)
                .build();

        return s3.putObject(putReq, AsyncRequestBody.fromBytes(data))
                .thenAccept(resp -> log.info("ProjectsContractStatus saved to S3/MinIO"))
                .exceptionally(ex -> {
                    log.error("Failed to save ProjectsContractStatus to S3/MinIO", ex);
                    return null;
                });
    }

    public void registerNewSignature(String project, KnownDocuments knownDocument, String cn, String organization) {
        var contractStatus = this.projects.computeIfAbsent(project, k -> new ContractStatus());
        var documentStatus = contractStatus.documents.computeIfAbsent(knownDocument, k -> new DocumentStatus());
        var signatureStatus = new SignatureStatus(Instant.now(), cn, organization);
        documentStatus.getSignatures().add(signatureStatus);
        saveAsync();
    }


    @PreDestroy
    public void shutdown() {
        try {
            saveAsync().join();
        } catch (Exception e) {
            log.error("Error while saving ProjectsContractStatus on shutdown", e);
        }
    }
}
