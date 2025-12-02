package edu.upc.dmag.signinginterface;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.EnumMap;
import java.util.Map;

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
}
