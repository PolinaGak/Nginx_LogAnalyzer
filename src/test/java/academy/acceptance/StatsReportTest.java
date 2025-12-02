package academy.acceptance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.*;

public class StatsReportTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("report_test");
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var stream = Files.walk(tempDir)) {
            stream.sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(java.io.File::delete);
        }
    }

    private Path createSampleLogFile(String content) throws IOException {
        Path logFile = tempDir.resolve("sample.log");
        Files.writeString(logFile, content);
        return logFile;
    }

    private int runApp(String... args) {
        return new picocli.CommandLine(new academy.Application()).execute(args);
    }

    @Test
    @DisplayName("Сохранение статистики в формате JSON")
    void jsonTest() throws IOException {
        String logContent = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET "
                + "/index.html HTTP/1.1\" 200 1234 \"-\" \"agent\"\n";
        Path logFile = createSampleLogFile(logContent);
        Path outFile = tempDir.resolve("report.json");

        int exitCode = runApp("--path", logFile.toString(), "--format", "json", "--output", outFile.toString());
        assertEquals(0, exitCode);
        assertTrue(Files.exists(outFile));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(outFile.toFile());
        assertEquals(1, root.get("totalRequestsCount").asInt());
        assertEquals(logFile.getFileName().toString(), root.get("files").get(0).asText());
    }

    @Test
    @DisplayName("Сохранение статистики в формате MARKDOWN")
    void markdownTest() throws IOException {
        String logContent = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET "
                + "/index.html HTTP/1.1\" 200 1234 \"-\" \"agent\"\n";
        Path logFile = createSampleLogFile(logContent);
        Path outFile = tempDir.resolve("report.md");

        int exitCode = runApp("--path", logFile.toString(), "--format", "markdown", "--output", outFile.toString());
        assertEquals(0, exitCode);
        assertTrue(Files.exists(outFile));

        String content = Files.readString(outFile);
        String fileName = logFile.getFileName().toString();

        assertThat(content).contains("#### Общая информация");
        assertThat(content).contains(fileName);
        assertThat(content).contains("200");
        assertThat(content).contains("OK");
        assertThat(content).contains("1");
        assertThat(content).contains("/index.html");

        assertThat(content).containsPattern("\\|\\s*200\\s*\\|.*OK.*\\|.*1\\s*\\|");
    }

    @Test
    @DisplayName("Сохранение статистики в формате ADOC")
    void adocTest() throws IOException {
        String logContent = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /index.html "
                + "HTTP/1.1\" 200 1234 \"-\" \"agent\"\n";
        Path logFile = createSampleLogFile(logContent);
        Path outFile = tempDir.resolve("report.ad");

        int exitCode = runApp("--path", logFile.toString(), "--format", "adoc", "--output", outFile.toString());
        assertEquals(0, exitCode, "Код возврата должен быть 0 при поддерживаемом формате adoc");
        assertTrue(Files.exists(outFile));

        String content = Files.readString(outFile);
        assertThat(content).contains("= Отчёт по логам");
    }
}
