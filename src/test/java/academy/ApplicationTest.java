package academy;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.*;
import picocli.CommandLine;

public class ApplicationTest {

    private Path tempDir;
    private Path logFile;
    private Path outputFile;

    private int runApp(String... args) {
        return new CommandLine(new academy.Application()).execute(args);
    }

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("application_test");

        String logContent = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /index.html "
                + "HTTP/1.1\" 200 1234 \"-\" \"Mozilla/5.0\"\n"
                + "93.180.71.4 - - [17/May/2015:08:06:32 +0000] \"GET /about.html "
                + "HTTP/1.1\" 404 0 \"-\" \"Mozilla/5.0\"\n";

        logFile = tempDir.resolve("access.log");
        Files.writeString(logFile, logContent);

        outputFile = tempDir.resolve("report.json");
    }

    @AfterEach
    void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Ошибка при удалении " + path + ": " + e.getMessage());
                    }
                });
            }
        }
    }

    @Test
    @DisplayName("Базовая проверка работоспособности программы")
    void happyPathTest() throws IOException {
        String[] args = {"--path", logFile.toString(), "--format", "json", "--output", outputFile.toString()};

        int exitCode = runApp(args);
        assertEquals(0, exitCode, "Программа должна завершиться с кодом 0");
        assertTrue(Files.exists(outputFile), "Выходной файл должен быть создан");
        assertTrue(Files.size(outputFile) > 0, "Выходной файл не должен быть пустым");

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(outputFile.toFile());

        assertEquals(2, root.get("totalRequestsCount").asInt(), "Общее количество запросов должно быть 2");
        assertTrue(root.has("files"), "JSON должен содержать поле 'files'");
        assertTrue(root.has("responseSizeInBytes"), "JSON должен содержать поле 'responseSizeInBytes'");
        assertTrue(root.has("resources"), "JSON должен содержать поле 'resources'");
        assertTrue(root.has("responseCodes"), "JSON должен содержать поле 'responseCodes'");
    }
}
