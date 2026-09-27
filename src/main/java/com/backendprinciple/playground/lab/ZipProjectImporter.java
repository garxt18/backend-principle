package com.backendprinciple.playground.lab;

import com.backendprinciple.playground.common.error.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import org.springframework.stereotype.Component;

/**
 * Reads an uploaded .zip of a project. The zip is spooled to a temp file (deleted right after) so its
 * size does not matter; only the source files we keep are decompressed, into memory.
 *
 * <p>Defences, because uploads are hostile until proven otherwise:
 * <ul>
 *   <li>Zip bomb: skipped entries are never decompressed, and for kept ones we count the bytes we
 *       actually decompress and stop at a hard limit, instead of trusting the sizes in the zip header.</li>
 *   <li>Zip slip: entry names with "..", absolute paths or backslashes are rejected.</li>
 *   <li>Binary/huge files and build output (target/, node_modules/, .git/ ...) are skipped.</li>
 * </ul>
 */
@Component
public class ZipProjectImporter {

    private static final Set<String> IGNORED_DIRS = Set.of("target", "build", "out", "bin", ".git", ".idea",
            ".vscode", ".gradle", "node_modules", ".mvn", "__MACOSX", ".settings", "dist", "logs",
            "_reference", // _reference = the originals inside a Rebuild Lab progress export
            ".next", ".vercel", ".turbo", "coverage", ".nuxt", ".svelte-kit");
    /** Generated or lock files: huge, not written by hand, nothing to learn from retyping them. */
    private static final Set<String> IGNORED_FILES = Set.of("package-lock.json", "pnpm-lock.yaml", "yarn.lock",
            "bun.lockb", "next-env.d.ts", "tsconfig.tsbuildinfo");

    private final LabProperties props;

    public ZipProjectImporter(LabProperties props) {
        this.props = props;
    }

    public record ImportedFile(String path, String content) {
    }

    public record ImportResult(List<ImportedFile> files, List<String> skipped) {
    }

    /** Spools the upload to a temp file first: see {@link #read(Path)} for why. */
    public ImportResult read(InputStream zipStream) {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("lab-upload-", ".zip");
            Files.copy(zipStream, tmp, StandardCopyOption.REPLACE_EXISTING);
            return read(tmp);
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file");
        } finally {
            deleteQuietly(tmp);
        }
    }

    /**
     * There is no limit on the size of the .zip itself: a real project zip is mostly node_modules/, .git/,
     * build output and images, and none of that is ever decompressed. {@link ZipFile} reads the zip's index
     * (central directory) and we only open the entries we keep - source files, each capped at
     * max-file-bytes. What is stored is capped by max-files-per-project and max-total-bytes; past those the
     * remaining files are skipped (and reported), the import itself still succeeds.
     */
    public ImportResult read(Path zipPath) {
        List<ImportedFile> files = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        int overLimit = 0;
        long totalBytes = 0;
        try (ZipFile zip = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String path = sanitize(entry.getName());
                if (path == null) {
                    report(skipped, entry.getName() + " (unsafe path)");
                    continue;
                }
                if (isIgnored(path) || !FileClassifier.isSupported(path)) {
                    continue;
                }
                if (files.size() >= props.maxFilesPerProject() || totalBytes >= props.maxTotalBytes()) {
                    overLimit++;
                    continue;
                }
                byte[] bytes;
                try (InputStream in = zip.getInputStream(entry)) {
                    bytes = readLimited(in, props.maxFileBytes());
                }
                if (bytes == null) {
                    report(skipped, path + " (larger than " + props.maxFileBytes() / 1000 + " KB)");
                    continue;
                }
                if (totalBytes + bytes.length > props.maxTotalBytes()) {
                    overLimit++;
                    continue;
                }
                String text = decodeUtf8(bytes);
                if (text == null || text.indexOf('\0') >= 0) {
                    report(skipped, path + " (not a UTF-8 text file)");
                    continue;
                }
                totalBytes += bytes.length;
                files.add(new ImportedFile(path, normalizeNewlines(text)));
            }
        } catch (ZipException e) {
            throw ApiException.badRequest("That is not a valid .zip file");
        } catch (IOException e) {
            throw ApiException.badRequest("Could not read the uploaded file");
        }
        if (overLimit > 0) {
            skipped.add(overLimit + " more source file" + (overLimit == 1 ? "" : "s") + " (project limit of "
                    + props.maxFilesPerProject() + " files / " + props.maxTotalBytes() / 1_000_000 + " MB of code reached)");
        }
        return new ImportResult(stripCommonRoot(files), skipped);
    }

    private static final int MAX_REPORTED = 50;

    /** Keeps the "skipped" list readable (and the response small) for zips with thousands of odd entries. */
    private static void report(List<String> skipped, String reason) {
        if (skipped.size() < MAX_REPORTED) {
            skipped.add(reason);
        }
    }

    private static void deleteQuietly(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
                // temp dir is cleaned by the OS eventually
            }
        }
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
        return name.equals(".DS_Store") || name.endsWith(".class") || name.endsWith(".jar")
                || IGNORED_FILES.contains(name) || path.contains("generated/prisma/");
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
