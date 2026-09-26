package com.backendprinciple.playground.lab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.backendprinciple.playground.common.error.ApiException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class ZipProjectImporterTest {

    private final ZipProjectImporter importer = new ZipProjectImporter(new LabProperties(15, 400, 1000, 5000));

    @Test
    void importsSourceFilesStripsCommonRootAndIgnoresBuildOutput() throws IOException {
        var result = importer.read(zip(Map.of(
                "shop-main/pom.xml", "<project/>",
                "shop-main/src/main/java/App.java", "class App {}\r\n",
                "shop-main/target/classes/App.class", "binary",
                "shop-main/node_modules/x/index.js", "x",
                "shop-main/.git/config", "[core]",
                "shop-main/logo.png", "png")));

        assertThat(result.files()).extracting(ZipProjectImporter.ImportedFile::path)
                .containsExactlyInAnyOrder("pom.xml", "src/main/java/App.java");
        assertThat(result.files()).filteredOn(f -> f.path().endsWith(".java"))
                .first().extracting(ZipProjectImporter.ImportedFile::content).isEqualTo("class App {}\n");
    }

    @Test
    void rejectsZipSlipPaths() throws IOException {
        var result = importer.read(zip(Map.of("../../etc/passwd.txt", "root", "ok/App.java", "class A {}")));
        assertThat(result.files()).extracting(ZipProjectImporter.ImportedFile::path).containsExactly("App.java");
        assertThat(result.skipped()).anyMatch(s -> s.contains("unsafe path"));
    }

    @Test
    void skipsOversizedFilesAndStopsZipBombs() throws IOException {
        var big = importer.read(zip(Map.of("Big.java", "x".repeat(1500), "Small.java", "class S {}")));
        assertThat(big.files()).extracting(ZipProjectImporter.ImportedFile::path).containsExactly("Small.java");
        assertThat(big.skipped()).anyMatch(s -> s.startsWith("Big.java"));

        var entries = new java.util.HashMap<String, String>();
        for (int i = 0; i < 8; i++) {
            entries.put("F" + i + ".java", "y".repeat(900));
        }
        assertThatThrownBy(() -> importer.read(zip(entries))).isInstanceOf(ApiException.class)
                .hasMessageContaining("too large");
    }

    @Test
    void rejectsGarbage() {
        var result = importer.read(new ByteArrayInputStream("definitely not a zip".getBytes(StandardCharsets.UTF_8)));
        assertThat(result.files()).isEmpty();
    }

    @Test
    void sanitizeBlocksTraversalAndAbsolutePaths() {
        assertThat(ZipProjectImporter.sanitize("a/../b")).isNull();
        assertThat(ZipProjectImporter.sanitize("/etc/x")).isNull();
        assertThat(ZipProjectImporter.sanitize("a\\b")).isNull();
        assertThat(ZipProjectImporter.sanitize("src/Main.java")).isEqualTo("src/Main.java");
    }

    private static ByteArrayInputStream zip(Map<String, String> files) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (var e : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue().getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
        }
        return new ByteArrayInputStream(out.toByteArray());
    }
}
