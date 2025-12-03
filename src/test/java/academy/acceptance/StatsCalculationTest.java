package academy.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.*;

public class StatsCalculationTest {

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("stats_calc");
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
    @DisplayName("Расчет статистики на основании локального log-файла")
    void happyPathTest() throws IOException {
        String logContent = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET "
                + "/downloads/product_1 HTTP/1.1\" 200 1000 \"-\" \"agent1\"\n"
                + "93.180.71.4 - - [17/May/2015:08:06:32 +0000] \"GET "
                + "/downloads/product_2 HTTP/1.1\" 200 500 \"-\" \"agent2\"\n"
                + "93.180.71.5 - - [18/May/2015:08:05:32 +0000] \"GET /index.html "
                + "HTTP/1.1\" 404 0 \"-\" \"agent3\"\n"
                + "93.180.71.6 - - [18/May/2015:08:07:32 +0000] \"GET "
                + "/downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"agent4\"\n";

        Path logFile = createSampleLogFile(logContent);
        Path outFile = tempDir.resolve("stats.json");

        int exitCode = runApp("--path", logFile.toString(), "--format", "json", "--output", outFile.toString());
        assertEquals(0, exitCode);
        assertTrue(Files.exists(outFile));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(outFile.toFile());

        assertEquals(375.0, root.path("responseSizeInBytes").path("average").asDouble(), 0.01, "Средний размер ответа");
        assertEquals(1000.0, root.path("responseSizeInBytes").path("max").asDouble(), "Максимальный размер ответа");

        JsonNode resources = root.path("resources");
        boolean foundProduct1 = false;
        for (JsonNode resource : resources) {
            if ("/downloads/product_1".equals(resource.path("resource").asText())
                    && resource.path("totalRequestsCount").asInt() == 2) {
                foundProduct1 = true;
            }
        }
        assertTrue(foundProduct1, "Ресурс /downloads/product_1 должен быть в топе с 2 запросами");

        JsonNode responseCodes = root.path("responseCodes");
        boolean has200 = false, has304 = false, has404 = false;
        for (JsonNode code : responseCodes) {
            int statusCode = code.path("code").asInt();
            if (statusCode == 200) has200 = true;
            if (statusCode == 304) has304 = true;
            if (statusCode == 404) has404 = true;
        }
        assertTrue(has200, "Должен быть код 200");
        assertTrue(has304, "Должен быть код 304");
        assertTrue(has404, "Должен быть код 404");
    }
}
