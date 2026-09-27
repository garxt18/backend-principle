package com.backendprinciple.playground.lab;

import com.backendprinciple.playground.common.error.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import org.springframework.stereotype.Component;

/**
 * Reads an uploaded .zip of a project fully in memory - nothing is ever written to disk.
 *
 * <p>Defences, because uploads are hostile until proven otherwise:
 * <ul>
 *   <li>Zip bomb: we count the bytes we actually decompress and stop at a hard limit, instead of
 *       trusting the sizes written in the zip header.</li>
 *   <li>Zip slip: entry names with "..", absolute paths or backslashes are rejected.</li>
 *   <li>Binary/huge files and build output (target/, node_modules/, .git/ ...) are skipped.</li>
 * </ul>
 */
@Component
public class ZipProjectImporter {

    private static final Set<String> IGNORED_DIRS = Set.of("target", "build", "out", "bin", ".git", ".idea",
            ".vscode", ".gradle", "node_modules", ".mvn", "__MACOSX", ".settings", "dist", "logs",
            "_reference"); // _reference = the originals inside a Rebuild Lab progress export

    private final LabProperties props;

    public ZipProjectImporter(LabProperties props) {
        this.props = props;
    }

    public record ImportedFile(String path, String content) {
    }

    public record ImportResult(List<ImportedFile> files, List<String> skipped) {
    }

    public ImportResult read(InputStream zipStream) {
        List<ImportedFile> files = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        long totalBytes = 0;
        try (ZipInputStream zip = new ZipInputStream(zipStream, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String path = sanitize(entry.getName());
                if (path == null) {
                    skipped.add(entry.getName() + " (unsafe path)");
                    continue;
                }
                if (isIgnored(path) || !FileClassifier.isSupported(path)) {
                    continue;
                }
                if (files.size() >= props.maxFilesPerProject()) {
                    skipped.add(path + " (file limit reached)");
                    continue;
                }
                byte[] bytes = readLimited(zip, props.maxFileBytes());
                if (bytes == null) {
                    skipped.add(path + " (larger than " + props.maxFileBytes() / 1000 + " KB)");
                    continue;
                }
                totalBytes += bytes.length;
                if (totalBytes > props.maxTotalBytes()) {
                    throw ApiException.badRequest("The project is too large once unzipped");
                }
                String text = decodeUtf8(bytes);
                if (text == null || text.indexOf('\0') >= 0) {
                    skipped.add(path + " (not a UTF-8 text file)");
                    continue;
                }
                files.add(new ImportedFile(path, normalizeNewlines(text)));
            }
        } catch (ZipException e) {
            throw ApiException.badRequest("That is not a valid .zip file");
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file");
        }
        return new ImportResult(stripCommonRoot(files), skipped);
    }

    /** Returns the bytes, or null if the entry is bigger than {@code limit} (we stop reading early). */
    private static byte[] readLimited(InputStream in, int limit) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int total = 0;
        int n;
        while ((n = in.read(buffer)) != -1) {
            total += n;
            if (total > limit) {
                return null;
            }
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    static String sanitize(String name) {
        if (name == null || name.isBlank() || name.contains("\\") || name.startsWith("/") || name.contains("\0")) {
            return null;
        }
        for (String part : name.split("/")) {
            if (part.equals("..") || part.equals(".")) {
                return null;
            }
        }
        return name.length() > 400 ? null : name;
    }

    private static boolean isIgnored(String path) {
        for (String part : path.split("/")) {
            if (IGNORED_DIRS.contains(part)) {
                return true;
            }
        }
        String name = FileClassifier.fileName(path);
        return name.equals(".DS_Store") || name.endsWith(".class") || name.endsWith(".jar");
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes)).toString()
                    .replace("﻿", "");
        } catch (CharacterCodingException e) {
            return null;
        }
    }

    private static String normalizeNewlines(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    /** GitHub zips wrap everything in "repo-main/": strip a folder that every file shares. */
    static List<ImportedFile> stripCommonRoot(List<ImportedFile> files) {
        List<ImportedFile> current = files;
        while (!current.isEmpty()) {
            String first = current.getFirst().path();
            int slash = first.indexOf('/');
            if (slash < 0) {
                return current;
            }
            String prefix = first.substring(0, slash + 1);
            if (!current.stream().allMatch(f -> f.path().startsWith(prefix))) {
                return current;
            }
            current = current.stream().map(f -> new ImportedFile(f.path().substring(prefix.length()), f.content())).toList();
        }
        return current;
    }
}
