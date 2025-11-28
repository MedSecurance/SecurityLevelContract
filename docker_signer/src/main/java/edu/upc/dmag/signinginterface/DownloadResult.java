package edu.upc.dmag.signinginterface;

public class DownloadResult {
    private final String versionId;
    private final String sha256Hash;
    private final String base64Data;

    public DownloadResult(String versionId, String sha256Hash, String base64Data) {
        this.versionId = versionId;
        this.sha256Hash = sha256Hash;
        this.base64Data = base64Data;
    }

    public String versionId() { return versionId; }
    public String sha256Hash() { return sha256Hash; }
    public String base64Data() { return base64Data; }
}
