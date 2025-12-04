package edu.upc.dmag.signinginterface;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
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

    @Override
    public String toString() {
        return "DocumentStatus{" +
                "timestamp=" + timestamp +
                ", hash='" + hash + '\'' +
                ", signatures=" + signatures +
                '}';
    }

    public SignatureStatus getSignatureByOrganization(String org) {
        if (org == null || this.signatures == null) return null;
        for (SignatureStatus s : this.signatures) {
            if (s != null && s.getOrganization() != null && s.getOrganization().equalsIgnoreCase(org)) {
                return s;
            }
        }
        return null;
    }

}
