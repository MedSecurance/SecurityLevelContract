package edu.upc.dmag.signinginterface;

public record DownloadResult(String versionId, String sha256Hash, String base64Data) {
}
