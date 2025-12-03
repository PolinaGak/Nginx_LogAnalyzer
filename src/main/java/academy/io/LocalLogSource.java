package academy.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalLogSource implements LogSource {
    private static final Logger logger = LogManager.getLogger(LocalLogSource.class);

    private final String pathPattern;
    private final List<BufferedReader> readers = new ArrayList<>();

    public LocalLogSource(String pathPattern) {
        this.pathPattern = pathPattern;
    }

    @Override
    public Stream<String> lines() {
        List<Path> matchedFiles = resolvePaths();
        if (matchedFiles.isEmpty()) {
            logger.warn("Не найдено файлов по шаблону: {}", pathPattern);
            return Stream.empty();
        }

        Stream<String> result = Stream.empty();
        for (Path file : matchedFiles) {
            try {
                if (!Files.isReadable(file)) {
                    logger.warn("Файл недоступен для чтения: {}", file);
                    continue;
                }

                BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8);
                readers.add(reader);
                result = Stream.concat(result, reader.lines());
            } catch (IOException e) {
                logger.error("Ошибка при чтении файла {}: {}", file, e.getMessage(), e);
            }
        }
        return result.onClose(this::close);
    }

    private List<Path> resolvePaths() {
        Path p = Path.of(pathPattern);
        if (Files.exists(p) && Files.isRegularFile(p)) {
            return List.of(p);
        } else {
            logger.warn("Файл не найден: {}", pathPattern);
            return List.of();
        }
    }

    @Override
    public String getSourceIdentifier() {
        return pathPattern;
    }

    @Override
    public void close() {
        for (BufferedReader reader : readers) {
            try {
                reader.close();
            } catch (IOException e) {
                logger.warn("Ошибка при закрытии reader'а: {}", e.getMessage());
            }
        }
        readers.clear();
    }
}
