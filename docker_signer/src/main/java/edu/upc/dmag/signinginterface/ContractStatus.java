package edu.upc.dmag.signinginterface;

import software.amazon.awssdk.services.s3.model.S3Object;

import java.util.EnumMap;
import java.util.Map;


public class ContractStatus {
    protected final Map<KnownDocuments, DocumentStatus> documents = new EnumMap<>(KnownDocuments.class);

    public ContractStatus() {
        for (KnownDocuments doc : KnownDocuments.values()) {
            documents.put(doc, null);
        }
    }
}
