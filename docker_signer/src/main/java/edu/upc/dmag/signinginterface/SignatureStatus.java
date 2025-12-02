package edu.upc.dmag.signinginterface;

import java.time.Instant;

public class SignatureStatus {
    public Instant signatureTime;
    public String actor;

    public SignatureStatus(Instant signatureTime, String actor) {
        this.signatureTime = signatureTime;
        this.actor = actor;
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
