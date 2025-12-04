package edu.upc.dmag.signinginterface;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@JsonIgnoreProperties(ignoreUnknown = true)
public class DocumentStatus {
    public Instant timestamp;
    public String eTag;
    public String hash;
    public List<SignatureStatus> signatures;

    public DocumentStatus(Instant timestamp, String eTag, String hash, List<SignatureStatus> signatures) {
        this.timestamp = timestamp;
        this.eTag = eTag;
        this.hash = hash;
        this.signatures = signatures;
    }

    public DocumentStatus() {
        this.signatures = new ArrayList<>();
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String geteTag() {
        return eTag;
    }

    public void seteTag(String eTag) {
        this.eTag = eTag;
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
                ", eTag='" + eTag + '\'' +
                ", hash='" + hash + '\'' +
                ", signatures=" + signatures +
                '}';
    }

    public SignatureStatus getSignatureByOrganization(String org) {
        log.error("Getting signature for organization: {} in document with signatures:", org);
        for (SignatureStatus s : this.signatures) {
            log.error(" - {}", s);
        }
        if (org == null || this.signatures == null) return null;
        for (SignatureStatus s : this.signatures) {
            if (s != null && s.getOrganization() != null && s.getOrganization().equalsIgnoreCase(org)) {
                return s;
            }
        }
        return null;
    }

}
