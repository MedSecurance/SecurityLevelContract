package edu.upc.dmag.signinginterface;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractStatus {
    protected Map<KnownDocuments, DocumentStatus> documents = new EnumMap<>(KnownDocuments.class);

    public ContractStatus() {
        for (KnownDocuments doc : KnownDocuments.values()) {
            documents.put(doc, null);
        }
    }

    @JsonProperty("documents")
    public Map<KnownDocuments, DocumentStatus> getDocuments() {
        return documents;
    }

    @JsonProperty("documents")
    public void setDocuments(Map<KnownDocuments, DocumentStatus> documents) {
        this.documents = documents;
    }

    public Set<String> getOrganizations() {
        Set<String> orgs = new HashSet<>();
        for (DocumentStatus docStatus : documents.values()) {
            if (docStatus != null) {
                for (SignatureStatus sigStatus : docStatus.getSignatures()) {
                    orgs.add(sigStatus.getOrganization());
                }
            }
        }
        return orgs;
    }
}
