package academy.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.*;

public class LogFileParsingTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("parsing_test");
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
    @DisplayName("На вход передан валидный локальный log-файл")
    void localFileProcessingTest() throws IOException {
        String logContent = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET "
                + "/downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3\"\n";
        Path logFile = createSampleLogFile(logContent);
        Path outFile = tempDir.resolve("report.json");

        int exitCode = runApp("--path", logFile.toString(), "--format", "json", "--output", outFile.toString());
        assertEquals(0, exitCode);
        assertTrue(Files.exists(outFile));
    }

    @Test
    @DisplayName("На вход передан валидный удаленный log-файл")
    void remoteFileProcessingTest() throws IOException {
        Path outFile = tempDir.resolve("remote.json");

        String validLogUrl = "https://gist.githubusercontent.com/PolinaGak/" + "6df5f8ffd6f92bb11ecec71b5255cc8a/raw";

        int exitCode = runApp("--path", validLogUrl, "--format", "json", "--output", outFile.toString());

        assertEquals(0, exitCode, "Код выхода должен быть 0 для валидного удаленного файла");
        assertTrue(Files.exists(outFile), "Файл отчета должен быть создан");
        assertTrue(Files.size(outFile) > 0, "Файл отчета не должен быть пустым");
    }

    @Test
    @DisplayName("На вход передан валидный локальный log-файл, часть строк в "
            + "котором нужно отфильтровать по --from и --to")
    void localFileProcessingAndFilteringTest() throws IOException {
        String logContent = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET "
                + "/downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"agent\"\n"
                + "93.180.71.3 - - [18/May/2015:08:05:32 +0000] \"GET "
                + "/downloads/product_2 HTTP/1.1\" 200 1234 \"-\" \"agent\"\n";
        Path logFile = createSampleLogFile(logContent);
        Path outFile = tempDir.resolve("filtered.json");

        int exitCode = runApp(
                "--path",
                logFile.toString(),
                "--format",
                "json",
                "--output",
                outFile.toString(),
                "--from",
                "2015-05-18");
        assertEquals(0, exitCode);
    }

    @Test
    @DisplayName("На вход передан локальный log-файл, часть строк в котором не " + "подходит под формат")
    void damagedLocalFileProcessingTest() throws IOException {
        String logContent = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET "
                + "/downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"agent\"\n"
                + "invalid log line\n"
                + "93.180.71.3 - - [18/May/2015:08:05:32 +0000] \"GET "
                + "/downloads/product_2 HTTP/1.1\" 200 1234 \"-\" \"agent\"\n";
        Path logFile = createSampleLogFile(logContent);
        Path outFile = tempDir.resolve("damaged.json");

        int exitCode = runApp("--path", logFile.toString(), "--format", "json", "--output", outFile.toString());
        assertEquals(0, exitCode);
        assertTrue(Files.exists(outFile));
    }
}
