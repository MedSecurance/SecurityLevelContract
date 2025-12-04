package edu.upc.dmag.signinginterface;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
        Set<String> organizations = new HashSet<>();
        for (DocumentStatus docStatus : documents.values()) {
            if (docStatus != null) {
                for (SignatureStatus sigStatus : docStatus.getSignatures()) {
                    organizations.add(sigStatus.getOrganization());
                }
            }
        }
        return organizations;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<KnownDocuments, DocumentStatus> entry : documents.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ");
            if (entry.getValue() != null) {
                sb.append(entry.getValue()).append("\n");
            } else {
                sb.append("null\n");
            }
        }
        return sb.toString();
    }
}
