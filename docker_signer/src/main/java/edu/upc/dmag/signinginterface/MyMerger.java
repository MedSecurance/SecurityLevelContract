package edu.upc.dmag.signinginterface;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

@Slf4j
public class MyMerger {
    public static void merge(File tmpFile, File newSignatureFile, File mergedFile) throws IOException {
        String prefix = "META-INF/signatures";
        try (
                ZipFile baseZipFile = new ZipFile(tmpFile);
                ZipFile newSignatureZipFile = new ZipFile(newSignatureFile);
                ZipOutputStream zos = new ZipOutputStream(new java.io.FileOutputStream(mergedFile))
        ){
            var currentEntries = extractZipEntries(baseZipFile, prefix);
            log.debug("Current entries before merging: {}", String.join(", ", currentEntries.keySet()));
            var newEntries = extractZipEntries(newSignatureZipFile, prefix);

            for (var entry: java.util.Collections.list(baseZipFile.entries())) {
                writeEntry(zos, entry, baseZipFile, null);
            }

            for(var newEntry: newEntries.entrySet() ) {
                int newName = Integer.parseInt(newEntry.getKey().replace(".xml", ""));
                log.debug("Initial new entry name is {} interpreting it as {} ", newEntry.getKey(), newName);
                while (currentEntries.containsKey(String.format("%03d", newName) + ".xml")) {
                    newName += 1;
                }
                currentEntries.put(String.format("%03d", newName) + ".xml", newEntry.getValue());
                writeEntry(zos, newEntry.getValue(), newSignatureZipFile, prefix+String.format("%03d", newName) + ".xml");
            }
            log.debug("Finished merging");
        };
    }

    private static void writeEntry(ZipOutputStream zos, ZipEntry entry, ZipFile baseZipFile, String newName) throws IOException {
        ZipEntry copiedEntry;
        copiedEntry = new ZipEntry(Objects.requireNonNullElseGet(newName, entry::getName));
        copiedEntry.setMethod(entry.getMethod());
        copiedEntry.setTime(entry.getTime());
        copiedEntry.setExtra(entry.getExtra());
        copiedEntry.setComment(entry.getComment());
        copiedEntry.setSize(entry.getSize());
        copiedEntry.setCompressedSize(entry.getCompressedSize());
        copiedEntry.setCrc(entry.getCrc());

        zos.putNextEntry(copiedEntry);
        try (var is = baseZipFile.getInputStream(entry)) {
            is.transferTo(zos);
        }
    }

    private static SortedMap<String, ZipEntry> extractZipEntries(ZipFile newSignatureZipFile, String folder) {
        SortedMap<String, ZipEntry> entriesToAdd = new TreeMap<>();
        for (ZipEntry entry : java.util.Collections.list(newSignatureZipFile.entries())) {
            String name = entry.getName();
            if (name.startsWith(folder) && !entry.isDirectory()) {
                entriesToAdd.put(name.substring(folder.length()), entry);
            }
        }
        return entriesToAdd;
    }
}
