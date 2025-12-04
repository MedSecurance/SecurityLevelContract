package edu.upc.dmag.signinginterface;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Setter
@Getter
public class SignatureStatus {
    public Instant signatureTime;
    public String actor;
    public String organization;

    public SignatureStatus() {
    }

    @Override
    public String toString() {
        return "SignatureStatus{" +
                "signatureTime=" + signatureTime +
                ", actor='" + actor + '\'' +
                ", organization='" + organization + '\'' +
                '}';
    }

    public SignatureStatus(Instant signatureTime, String actor, String organization) {
        this.signatureTime = signatureTime;
        this.actor = actor;
        this.organization = organization;
    }

}
