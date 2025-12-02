package edu.upc.dmag.signinginterface;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class DocumentStatus {
    public Instant timestamp;
    public String hash;
    public List<SignatureStatus> signatures;

    public DocumentStatus(Instant timestamp, String hash, List<SignatureStatus> signatures) {
        this.timestamp = timestamp;
        this.hash = hash;
        this.signatures = signatures;
    }

    public DocumentStatus() {
        this.signatures = new ArrayList<>();
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public List<SignatureStatus> getSignatures() {
        return signatures;
    }

    public void setSignatures(List<SignatureStatus> signatures) {
        this.signatures = signatures;
    }
}
