package academy;

import academy.output.Reporter;
import academy.output.ReporterFactory;
import academy.stats.LogProcessor;
import academy.stats.LogStatistics;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.*;

@Command(
        name = "log-analyzer",
        version = "Log Analyzer 1.0",
        mixinStandardHelpOptions = true,
        description = "Анализатор логов NGINX")
public class Application implements Callable<Integer> {

    private static final Logger logger = LogManager.getLogger(Application.class);

    @Option(
            names = {"--path", "-p"},
            required = true,
            description = "Путь к лог-файлам")
    private List<String> paths;

    @Option(
            names = {"--format", "-f"},
            required = true,
            description = "Формат вывода: json, markdown, adoc")
    private String format;

    @Option(
            names = {"--output", "-o"},
            required = true,
            description = "Путь к файлу отчёта")
    private Path output;

    @Option(names = "--from", description = "Начальная дата (ISO8601, например: 2025-03-01)")
    private String fromStr;

    @Option(names = "--to", description = "Конечная дата (ISO8601, например: 2025-03-31)")
    private String toStr;

    @Override
    public Integer call() {
        LocalDate from = null;
        LocalDate to = null;

        if (fromStr != null && !fromStr.trim().isEmpty()) {
            try {
                from = LocalDate.parse(fromStr.trim());
            } catch (DateTimeParseException e) {
                System.err.println("Неверный формат даты --from: " + fromStr);
                return 2;
            }
        }

        if (toStr != null && !toStr.trim().isEmpty()) {
            try {
                to = LocalDate.parse(toStr.trim());
            } catch (DateTimeParseException e) {
                System.err.println("Неверный формат даты --to: " + toStr);
                return 2;
            }
        }

        if (from != null && to != null && !from.isBefore(to)) {
            System.err.println("--from должна быть строго раньше --to");
            return 2;
        }

        try {
            validateOutputFile();
            validateFormat();

            for (String path : paths) {
                if (isUrl(path)) {
                    checkUrlExists(path);
                } else {
                    validateLocalPath(path);
                }
            }

            List<String> resolvedPaths = resolveGlobs(paths);
            LogProcessor processor = new LogProcessor(resolvedPaths, from, to);
            LogStatistics stats = processor.process();

            if (stats.getTotalRequestsCount() == 0) {
                logger.warn("Ни одна запись не была распарсена");
            }

            Reporter reporter = ReporterFactory.create(format);
            reporter.write(stats, output);

            logger.info("Анализ завершён. Отчёт сохранён: {}", output);
            return 0;

        } catch (IllegalArgumentException e) {
            System.err.println("Ошибка: " + e.getMessage());
            return 2;
        } catch (Exception e) {
            logger.error("Непредвиденная ошибка", e);
            System.err.println("Внутренняя ошибка: " + e.getMessage());
            return 1;
        }
    }

    private void validateOutputFile() {
        if (Files.exists(output)) {
            throw new IllegalArgumentException("Файл отчёта уже существует: " + output);
        }

        String ext = getFileExtension(output).toLowerCase();
        String fmt = format.toLowerCase();
        if (("json".equals(fmt) && !".json".equals(ext))
                || ("markdown".equals(fmt) && !".md".equals(ext))
                || ("adoc".equals(fmt) && !".ad".equals(ext))) {
            throw new IllegalArgumentException("Несоответствие формата и расширения: " + format + " → " + ext);
        }

        Path parent = output.getParent();
        if (parent == null) {
            parent = Path.of(".");
        }
        if (!Files.isWritable(parent)) {
            throw new IllegalArgumentException("Директория недоступна для записи: " + parent);
        }
    }

    private void validateFormat() {
        if (!List.of("json", "markdown", "adoc").contains(format.toLowerCase())) {
            throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
        }
    }

    private void validateLocalPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Путь не может быть null");
        }

        if (path.contains("*")) {
            return;
        }

        Path p = Path.of(path);
        if (!Files.exists(p)) {
            throw new IllegalArgumentException(String.format("Файл '%s' не найден", path));
        }

        Path fileName = p.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException(String.format("Некорректный путь: %s", path));
        }

        String name = fileName.toString().toLowerCase();
        if (!name.endsWith(".log") && !name.endsWith(".txt")) {
            throw new IllegalArgumentException(String.format("Файл должен иметь расширение .log или .txt: %s", path));
        }
    }

    private void checkUrlExists(String url) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (log-analyzer)")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());

            if (response.statusCode() >= 400) {
                throw new IllegalArgumentException(
                        "Удалённый файл недоступен: " + url + " (статус: " + response.statusCode() + ")");
            }
        } catch (IOException | InterruptedException e) {
            throw new IllegalArgumentException("Недоступный URL: " + url + " (" + e.getMessage() + ")");
        }
    }

    private boolean isUrl(String s) {
        return s != null && (s.startsWith("http://") || s.startsWith("https://"));
    }

    private String getFileExtension(Path path) {
        if (path == null || path.getFileName() == null) {
            return "";
        }
        String fileName = path.getFileName().toString();
        int i = fileName.lastIndexOf('.');
        return i == -1 ? "" : fileName.substring(i);
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Application()).execute(args);
        System.exit(exitCode);
    }

    private List<String> resolveGlobs(List<String> rawPaths) {
        List<String> resolved = new ArrayList<>();
        for (String path : rawPaths) {
            if (isUrl(path)) {
                resolved.add(path);
            } else if (path.contains("*") || path.contains("?")) {
                // Это glob-паттерн — раскрываем
                Path patternPath = Path.of(path);
                Path parent = Optional.ofNullable(patternPath.getParent()).orElse(Path.of("."));
                String fileNamePattern = patternPath.getFileName().toString();

                try {
                    FileSystem fs = FileSystems.getDefault();
                    PathMatcher matcher = fs.getPathMatcher("glob:" + parent.resolve(fileNamePattern));

                    // Используем walk с глубиной 1, как в LocalLogSource
                    List<Path> matches = Files.walk(parent, 1)
                            .filter(Files::isRegularFile)
                            .filter(p -> {
                                // На Windows — игнорируем регистр
                                if (System.getProperty("os.name").toLowerCase().contains("win")) {
                                    return p.getFileName()
                                                    .toString()
                                                    .toLowerCase()
                                                    .equals(fileNamePattern
                                                            .toLowerCase()
                                                            .replace("*", "")
                                                            .replace("?", ""))
                                            || fs.getPathMatcher("glob:" + fileNamePattern)
                                                    .matches(p.getFileName());
                                } else {
                                    return matcher.matches(p);
                                }
                            })
                            .collect(Collectors.toList());

                    if (matches.isEmpty()) {
                        logger.warn("Не найдено файлов по шаблону: {}", path);
                    }
                    matches.stream().map(Path::toString).forEach(resolved::add);
                } catch (IOException e) {
                    logger.error("Ошибка при раскрытии шаблона {}: {}", path, e.getMessage());
                    resolved.add(path); // fallback, хотя лучше выбросить
                }
            } else {
                // Обычный путь — оставляем как есть
                resolved.add(path);
            }
        }
        return resolved;
    }
}
