package academy.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

public class ArgumentValidationTest {

    private static final String[] SAMPLE_LOG_LINES = {
        "93.180.71.3 - - [17/May/2015:08:05:32 +0000] \"GET "
                + "/downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3 "
                + "(0.8.16~exp12ubuntu10.21)\"",
        "93.180.71.3 - - [17/May/2015:08:05:23 +0000] \"GET "
                + "/downloads/product_1 HTTP/1.1\" 304 0 \"-\" \"Debian APT-HTTP/1.3 "
                + "(0.8.16~exp12ubuntu10.21)\""
    };

    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("test");
    }

    @AfterEach
    void tearDown() throws IOException {
        try (var stream = Files.walk(tempDir)) {
            stream.sorted(java.util.Comparator.reverseOrder()).map(Path::toFile).forEach(java.io.File::delete);
        }
    }

    private Path createSampleLogFile(String... lines) throws IOException {
        Path logFile = tempDir.resolve("sample.log");
        Files.write(logFile, java.util.Arrays.asList(lines));
        return logFile;
    }

    private int runApp(String... args) {
        return new picocli.CommandLine(new academy.Application()).execute(args);
    }

    @Test
    @DisplayName("На вход передан несуществующий локальный файл")
    void test1() {
        int exitCode = runApp(
                "--path",
                "nonexistent.log",
                "--format",
                "json",
                "--output",
                tempDir.resolve("out.json").toString());
        assertEquals(2, exitCode);
    }

    @Test
    @DisplayName("На вход передан несуществующий удаленный файл")
    void test2() {
        int exitCode = runApp(
                "--path",
                "https://httpstat.us/404  ",
                "--format",
                "json",
                "--output",
                tempDir.resolve("out.json").toString());
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {".docx", ".pdf"})
    @DisplayName("На вход передан файл в неподдерживаемом формате")
    void test3(String extension) throws IOException {
        Path badFile = tempDir.resolve("file" + extension);
        Files.createFile(badFile);
        int exitCode = runApp(
                "--path",
                badFile.toString(),
                "--format",
                "json",
                "--output",
                tempDir.resolve("out.json").toString());
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"2025.01.01", "today", "2025/13/01", "invalid-date"})
    @DisplayName("На вход переданы невалидные параметры --from / --to - {0}")
    void test4(String date) throws IOException {
        Path logFile = createSampleLogFile(SAMPLE_LOG_LINES);
        String[] args = {
            "--path",
            logFile.toString(),
            "--format",
            "json",
            "--output",
            tempDir.resolve("out.json").toString(),
            "--from",
            date
        };
        int exitCode = runApp(args);
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @MethodSource("test6ArgumentsSource")
    @DisplayName("По пути в аргументе --output указан файл с некорректным расширением")
    void test6(String format, String output) throws IOException {
        Path logFile = createSampleLogFile(SAMPLE_LOG_LINES);
        int exitCode = runApp(
                "--path",
                logFile.toString(),
                "--format",
                format,
                "--output",
                tempDir.resolve(output).toString());
        assertEquals(2, exitCode);
    }

    @Test
    @DisplayName("По пути в аргументе --output уже существует файл")
    void test7() throws IOException {
        Path existing = tempDir.resolve("existing.json");
        Files.createFile(existing);
        Path logFile = createSampleLogFile(SAMPLE_LOG_LINES);
        int exitCode = runApp("--path", logFile.toString(), "--format", "json", "--output", existing.toString());
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--path", "--output", "--format", "-p", "-o", "-f"})
    @DisplayName("На вход не передан обязательный параметр \"{0}\"")
    void test8(String missingArg) throws IOException {
        Path logFile = createSampleLogFile(SAMPLE_LOG_LINES);
        String[] baseArgs = {
            "--path", logFile.toString(),
            "--format", "json",
            "--output", tempDir.resolve("out.json").toString()
        };

        java.util.List<String> argList = new java.util.ArrayList<>();
        for (int i = 0; i < baseArgs.length; i += 2) {
            if (!baseArgs[i].equals(missingArg)
                    && !(missingArg.equals("-p") && baseArgs[i].equals("--path"))
                    && !(missingArg.equals("-f") && baseArgs[i].equals("--format"))
                    && !(missingArg.equals("-o") && baseArgs[i].equals("--output"))) {
                argList.add(baseArgs[i]);
                argList.add(baseArgs[i + 1]);
            }
        }
        int exitCode = runApp(argList.toArray(new String[0]));
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--input", "--filter"})
    @DisplayName("На вход передан неподдерживаемый параметр \"{0}\"")
    void test9(String arg) throws IOException {
        Path logFile = createSampleLogFile(SAMPLE_LOG_LINES);
        int exitCode = runApp(
                "--path",
                logFile.toString(),
                "--format",
                "json",
                "--output",
                tempDir.resolve("out.json").toString(),
                arg,
                "value");
        assertEquals(2, exitCode);
    }

    @Test
    @DisplayName("Значение параметра --from больше, чем значение параметра --to")
    void test10() throws IOException {
        Path logFile = createSampleLogFile(SAMPLE_LOG_LINES);
        int exitCode = runApp(
                "--path",
                logFile.toString(),
                "--format",
                "json",
                "--output",
                tempDir.resolve("out.json").toString(),
                "--from",
                "2025-02-01",
                "--to",
                "2025-01-01");
        assertEquals(2, exitCode);
    }

    private static Stream<Arguments> test6ArgumentsSource() {
        return Stream.of(
                Arguments.of("markdown", "results.txt"),
                Arguments.of("json", "results.md"),
                Arguments.of("adoc", "results.ad1"));
    }
}
