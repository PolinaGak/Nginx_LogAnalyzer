package academy.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LocalLogSource implements LogSource {
  private static final Logger logger =
      LogManager.getLogger(LocalLogSource.class);

  private final String pathPattern;
  private final List<BufferedReader> readers = new ArrayList<>();

  public LocalLogSource(String pathPattern) { this.pathPattern = pathPattern; }

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

        BufferedReader reader =
            Files.newBufferedReader(file, StandardCharsets.UTF_8);
        readers.add(reader);

        String firstLine = reader.readLine();
        if (firstLine != null) {
          logger.debug("Первая строка файла {}: '{}'", file, firstLine);
          result = Stream.concat(
              result, Stream.concat(Stream.of(firstLine), reader.lines()));
        } else {
          result = Stream.concat(result, reader.lines());
        }

      } catch (IOException e) {
        logger.error("Ошибка при чтении файла {}: {}", file, e.getMessage(), e);
      }
    }
    return result.onClose(this::close);
  }
  private List<Path> resolvePaths() {
    Path path = Paths.get(pathPattern);
    Path parent = path.getParent();
    if (parent == null) {
      parent = Paths.get(".");
    }

    String fileName = path.getFileName().toString();
    FileSystem fs = FileSystems.getDefault();
    PathMatcher matcher = fs.getPathMatcher("glob:" + fileName);

    boolean isWindows =
        System.getProperty("os.name").toLowerCase().contains("win");

    logger.debug("Ищем файлы: parent={}, fileName={}", parent, fileName);
    logger.debug("Рабочая директория: {}", Paths.get("").toAbsolutePath());
    logger.debug("Полный путь к родительской папке: {}",
                 parent.toAbsolutePath());

    try {
      if (!Files.exists(parent)) {
        logger.warn("Родительская директория не существует: {}",
                    parent.toAbsolutePath());
        return List.of();
      }

      if (!Files.isDirectory(parent)) {
        logger.warn("Указанный путь не является директорией: {}",
                    parent.toAbsolutePath());
        return List.of();
      }

      logger.debug("Список всех файлов в папке {}:", parent.toAbsolutePath());
      try {
        Files.list(parent)
            .filter(Files::isRegularFile)
            .forEach(p -> logger.debug("  - {}", p.getFileName()));
      } catch (IOException listEx) {
        logger.error("Не удалось прочитать содержимое директории {}: {}",
                     parent.toAbsolutePath(), listEx.getMessage(), listEx);
      }

      List<Path> matchedFiles = Files.walk(parent, 1)
                                    .filter(Files::isRegularFile)
                                    .filter(file -> {
                                      String name =
                                          file.getFileName().toString();
                                      if (isWindows) {
                                        return name.equalsIgnoreCase(fileName);
                                      } else {
                                        return matcher.matches(file);
                                      }
                                    })
                                    .collect(Collectors.toList());

      logger.debug("Найдено файлов по паттерну '{}': {}", fileName,
                   matchedFiles.size());
      matchedFiles.forEach(p -> logger.debug("  - {}", p.toAbsolutePath()));

      return matchedFiles;

    } catch (IOException e) {
      logger.error("Ошибка при поиске файлов по шаблону {}: {}", pathPattern,
                   e.getMessage(), e);
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