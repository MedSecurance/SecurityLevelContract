package edu.upc.dmag.signinginterface;

import java.io.File;

public record DownloadResult(String versionId, String sha256Hash, File file) {
}
