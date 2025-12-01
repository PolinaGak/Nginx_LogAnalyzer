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
                "https://httpstat.us/404",
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
    void test4(String date) {
        String[] args = {
            "--path",
            "logs/access.log",
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
    void test6(String format, String output) {
        int exitCode = runApp(
                "--path",
                "logs/access.log",
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
        int exitCode = runApp("--path", "logs/access.log", "--format", "json", "--output", existing.toString());
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--path", "--output", "--format", "-p", "-o", "-f"})
    @DisplayName("На вход не передан обязательный параметр \"{0}\"")
    void test8(String missingArg) {
        String[] args = {
            "--path", "logs/access.log",
            "--format", "json",
            "--output", tempDir.resolve("out.json").toString()
        };
        java.util.List<String> argList = new java.util.ArrayList<>();
        for (int i = 0; i < args.length; i += 2) {
            if (!args[i].equals(missingArg)
                    && !(missingArg.equals("-p") && args[i].equals("--path"))
                    && !(missingArg.equals("-f") && args[i].equals("--format"))
                    && !(missingArg.equals("-o") && args[i].equals("--output"))) {
                argList.add(args[i]);
                argList.add(args[i + 1]);
            }
        }
        int exitCode = runApp(argList.toArray(new String[0]));
        assertEquals(2, exitCode);
    }

    @ParameterizedTest
    @ValueSource(strings = {"--input", "--filter"})
    @DisplayName("На вход передан неподдерживаемый параметр \"{0}\"")
    void test9(String arg) {
        int exitCode = runApp(
                "--path",
                "logs/access.log",
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
    void test10() {
        int exitCode = runApp(
                "--path",
                "logs/access.log",
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
