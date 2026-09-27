package com.backendprinciple.playground.lab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LabSyncServiceTest {

    private static final List<String> ORIGINAL = List.of(
            "package demo;",
            "",
            "public class Hello {",
            "    public String greet() {",
            "        return \"hi\";",
            "    }",
            "}");

    @Test
    void countsMatchingLinesIgnoringIndentationAndBlankLines() {
        var c = LabSyncService.compare(ORIGINAL, List.of("package demo;", "public class Hello {", "  public   String greet() {"));
        assertThat(c.matchedLines()).isEqualTo(4);
        assertThat(c.mismatchLine()).isNull();
    }

    @Test
    void stopsAtTheFirstDifferentLineAndReportsIt() {
        var c = LabSyncService.compare(ORIGINAL, List.of("package demo;", "", "public class Helo {"));
        assertThat(c.matchedLines()).isEqualTo(1);
        assertThat(c.mismatchLine()).isEqualTo(3);
        assertThat(c.expected()).isEqualTo("public class Hello {");
        assertThat(c.found()).isEqualTo("public class Helo {");
    }

    @Test
    void aFullyTypedFileCountsEveryLine() {
        assertThat(LabSyncService.compare(ORIGINAL, ORIGINAL).matchedLines()).isEqualTo(ORIGINAL.size());
    }

    @Test
    void slugsAreSafeFileNames() {
        assertThat(LabSyncService.slug("Task Manager API (v1)!")).isEqualTo("task-manager-api-v1");
        assertThat(LabSyncService.slug("***")).isEqualTo("project");
    }
}
