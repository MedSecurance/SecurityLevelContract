package edu.upc.dmag.signinginterface;

import java.time.Instant;

public class SignatureStatus {
    public Instant signatureTime;
    public String actor;
    public String organization;

    public SignatureStatus() {
    }

    public SignatureStatus(Instant signatureTime, String actor, String organization) {
        this.signatureTime = signatureTime;
        this.actor = actor;
        this.organization = organization;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public Instant getSignatureTime() {
        return signatureTime;
    }

    public void setSignatureTime(Instant signatureTime) {
        this.signatureTime = signatureTime;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }
}
