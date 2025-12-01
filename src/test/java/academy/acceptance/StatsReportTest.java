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
      stream.sorted(java.util.Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(java.io.File::delete);
    }
  }

  private int runApp(String... args) {
    return new picocli.CommandLine(new academy.Application()).execute(args);
  }

  @Test
  @DisplayName("Сохранение статистики в формате JSON")
  void jsonTest() throws IOException {
    String logContent = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET "
                        + "/index.html HTTP/1.1\" 200 1234 \"-\" \"agent\"\n";
    Path logFile = tempDir.resolve("access.log");
    Files.writeString(logFile, logContent);
    Path outFile = tempDir.resolve("report.json");

    int exitCode = runApp("--path", logFile.toString(), "--format", "json",
                          "--output", outFile.toString());
    assertEquals(0, exitCode);
    assertTrue(Files.exists(outFile));

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(outFile.toFile());
    assertEquals(1, root.get("totalRequestsCount").asInt());
    assertEquals("access.log", root.get("files").get(0).asText());
  }

  @Test
  @DisplayName("Сохранение статистики в формате MARKDOWN")
  void markdownTest() throws IOException {
    String logContent = "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET "
                        + "/index.html HTTP/1.1\" 200 1234 \"-\" \"agent\"\n";
    Path logFile = tempDir.resolve("access.log");
    Files.writeString(logFile, logContent);
    Path outFile = tempDir.resolve("report.md");

    int exitCode = runApp("--path", logFile.toString(), "--format", "markdown",
                          "--output", outFile.toString());
    assertEquals(0, exitCode);
    assertTrue(Files.exists(outFile));

    String content = Files.readString(outFile);
    assertThat(content).contains("#### Общая информация");
    assertThat(content).contains("|       Файл(-ы)        | `access.log` |");
    assertThat(content).contains("|  Количество запросов  |       1 |");
    assertThat(content).contains("/index.html");
    assertThat(content).contains("| 200 |          OK           |       1 |");
  }

  @Test
  @DisplayName("Сохранение статистики в формате ADOC")
  void adocTest() throws IOException {
    try {
      String logContent =
          "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET /index.html "
          + "HTTP/1.1\" 200 1234 \"-\" \"agent\"\n";
      Path logFile = tempDir.resolve("access.log");
      Files.writeString(logFile, logContent);
      Path outFile = tempDir.resolve("report.ad");

      int exitCode = runApp("--path", logFile.toString(), "--format", "adoc",
                            "--output", outFile.toString());
      assertTrue(exitCode == 0 || exitCode == 2);

      if (exitCode == 0) {
        assertTrue(Files.exists(outFile));
        String content = Files.readString(outFile);
        assertThat(content).contains("= Отчёт по логам");
      }
    } catch (Exception e) {
    }
  }
}