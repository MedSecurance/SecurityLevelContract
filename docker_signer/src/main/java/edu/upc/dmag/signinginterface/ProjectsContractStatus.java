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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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

    @Value("${INSTANCE_ROLE:}")
    private String instanceRole;

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
        if (!"provider".equalsIgnoreCase(instanceRole)) {
            log.info("Skipping ProjectsContractStatus init because instance role is not 'provider' (is '{}')", instanceRole);
            return;
        }
        ObjectMapper mapper = createMapper();

        GetObjectRequest req = GetObjectRequest.builder()
                .bucket(BUCKET)
                .key(KEY)
                .build();

        try {
            ResponseBytes<GetObjectResponse> bytes = s3.getObject(req, AsyncResponseTransformer.toBytes()).join();
            byte[] data = bytes.asByteArray();

            log.error("data: {}",bytes.asString(StandardCharsets.UTF_8));
            TypeReference<Map<String, ContractStatus>> typeRef = new TypeReference<>() {};
            Map<String, ContractStatus> fromS3 = mapper.readValue(data, typeRef);
            log.error("ProjectsContractStatus loaded data: {}",fromS3);
            for (Map.Entry<String, ContractStatus> entry : fromS3.entrySet()) {
                log.error("Project: {} -> {}", entry.getKey(), entry.getValue().toString());
            }


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





    public void registerNewDocumentVersion(String project, KnownDocuments knownDocument, S3Object s3Object, String sha256) {
        if (!"provider".equalsIgnoreCase(instanceRole)) {
            log.info("Skipping ProjectsContractStatus save because instance role is not 'provider' (is '{}')", instanceRole);
            return;
        }
        var contractStatus = this.projects.computeIfAbsent(project, k -> new ContractStatus());
        var documentStatus = contractStatus.documents.computeIfAbsent(knownDocument, k -> new DocumentStatus());
        documentStatus.setTimestamp(s3Object.lastModified());
        documentStatus.setHash(sha256);
        documentStatus.seteTag(s3Object.eTag());
        documentStatus.signatures.clear();
        saveAsync();

    }

    public Set<String> getOrganizationsForProject(String project) {
        if (!"provider".equalsIgnoreCase(instanceRole)) {
            log.info("Skipping ProjectsContractStatus save because instance role is not 'provider' (is '{}')", instanceRole);
            return null;
        }
        var contractStatus = this.projects.computeIfAbsent(project, k-> new ContractStatus());
        return contractStatus.getOrganizations();
    }

    public Map<KnownDocuments, DocumentStatus> getDocumentsStatusForProject(String project) {
        if (!"provider".equalsIgnoreCase(instanceRole)) {
            log.info("Skipping ProjectsContractStatus save because instance role is not 'provider' (is '{}')", instanceRole);
            return null;
        }
        var contractStatus = this.projects.computeIfAbsent(project, k-> new ContractStatus());
        return contractStatus.getDocuments();
    }


    public CompletableFuture<Void> saveAsync() {
        if (!"provider".equalsIgnoreCase(instanceRole)) {
            log.info("Skipping ProjectsContractStatus save because instance role is not 'provider' (is '{}')", instanceRole);
            return CompletableFuture.completedFuture(null);
        }
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
        if (!"provider".equalsIgnoreCase(instanceRole)) {
            log.info("Skipping ProjectsContractStatus save because instance role is not 'provider' (is '{}')", instanceRole);
            return;
        }
        var contractStatus = this.projects.computeIfAbsent(project, k -> new ContractStatus());
        var documentStatus = contractStatus.documents.computeIfAbsent(knownDocument, k -> new DocumentStatus());
        var signatureStatus = new SignatureStatus(Instant.now(), cn, organization);
        documentStatus.getSignatures().add(signatureStatus);
        saveAsync();
    }


    @PreDestroy
    public void shutdown() {
        if (!"provider".equalsIgnoreCase(instanceRole)) {
            log.info("Skipping ProjectsContractStatus save because instance role is not 'provider' (is '{}')", instanceRole);
            return;
        }

        try {
            log.error("Saving ProjectsContractStatus on shutdown...");
            log.error("ProjectsContractStatus save result: {}",this.projects);
            for (Map.Entry<String, ContractStatus> entry : this.projects.entrySet()) {
                log.error("Project: {} -> {}", entry.getKey(), entry.getValue().toString());
            }
            saveAsync().join();
        } catch (Exception e) {
            log.error("Error while saving ProjectsContractStatus on shutdown", e);
        }
    }

    public boolean checkDocumentHash(String project, KnownDocuments knownDocuments, String sha256) {
        if (!"provider".equalsIgnoreCase(instanceRole)) {
            log.info("Skipping ProjectsContractStatus save because instance role is not 'provider' (is '{}')", instanceRole);
            return false;
        }
        if (!this.projects.containsKey(project)) {
            return false;
        }
        var contractStatus = this.projects.get(project);
        if (!contractStatus.documents.containsKey(knownDocuments)) {
            return false;
        }
        var documentStatus = contractStatus.documents.get(knownDocuments);
        return documentStatus.getHash() != null && documentStatus.getHash().equalsIgnoreCase(sha256);
    }

    public String getDocumentHash(String project, KnownDocuments knownDocuments) {
        if (!"provider".equalsIgnoreCase(instanceRole)) {
            log.info("Skipping ProjectsContractStatus save because instance role is not 'provider' (is '{}')", instanceRole);
            return null;
        }
        if (!this.projects.containsKey(project)) {
            return null;
        }
        var contractStatus = this.projects.get(project);
        if (!contractStatus.documents.containsKey(knownDocuments)) {
            return null;
        }
        var documentStatus = contractStatus.documents.get(knownDocuments);
        return documentStatus.getHash();
    }
}
