package academy.stats;

import academy.io.LogSource;
import academy.io.LogSourceFactory;
import academy.parsing.LogEntry;
import academy.parsing.NginxLogParser;
import academy.util.Percentile;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LogProcessor {
    private static final Logger logger = LogManager.getLogger(LogProcessor.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int TOP_RESOURCES_LIMIT = 10;

    private final List<String> sourcePaths;
    private final LocalDate fromDate;
    private final LocalDate toDate;

    public LogProcessor(List<String> sourcePaths, LocalDate fromDate, LocalDate toDate) {
        this.sourcePaths = Objects.requireNonNullElse(sourcePaths, List.of());
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    public LogStatistics process() {
        logger.info("Начало обработки {} источников", sourcePaths.size());

        NginxLogParser parser = new NginxLogParser();
        List<LogEntry> allEntries = new ArrayList<>();

        for (String path : sourcePaths) {
            logger.debug("Обработка источника: {}", path);
            try (LogSource source = LogSourceFactory.from(path)) {
                source.lines()
                        .peek(line -> logger.trace("Строка из источника '{}': {}", path, line))
                        .map(parser::parse)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .filter(entry -> isWithinDateRange(entry.getTimestamp().toLocalDate()))
                        .forEach(allEntries::add);
            } catch (Exception e) {
                logger.error("Ошибка при обработке источника {}: {}", path, e.getMessage(), e);
                System.exit(2);
            }
        }

        if (allEntries.isEmpty()) {
            logger.warn("Ни одна запись не была распарсена из {} источников", sourcePaths.size());
            logger.warn("Проверьте:");
            logger.warn("1. Кодировку файлов (должна быть UTF-8)");
            logger.warn("2. Формат логов (должен соответствовать формату NGINX)");
            logger.warn("3. Наличие BOM в начале файла");
        }

        LogStatistics stats = new LogStatistics();

        List<String> fileNames = sourcePaths.stream()
                .map(p -> {
                    if (p.startsWith("http://") || p.startsWith("https://")) {
                        String[] parts = p.split("/");
                        if (parts.length > 0 && !parts[parts.length - 1].isEmpty()) {
                            return parts[parts.length - 1];
                        } else {
                            return p;
                        }
                    } else {
                        // ИСПРАВЛЕНО: Paths.get() -> Path.of()
                        Path path = Path.of(p);
                        return path.getFileName() != null ? path.getFileName().toString() : p;
                    }
                })
                .collect(Collectors.toList());
        stats.setFiles(fileNames);

        int totalCount = allEntries.size();
        stats.setTotalRequestsCount(totalCount);
        logger.debug("Всего распарсено записей: {}", totalCount);

        if (totalCount == 0) {
            stats.setResources(List.of());
            stats.setResponseCodes(List.of());
            stats.setUniqueProtocols(List.of());
            stats.setRequestsPerDate(List.of());

            LogStatistics.ResponseSize sizeStats = new LogStatistics.ResponseSize();
            sizeStats.setAverage(0.0);
            sizeStats.setMax(0.0);
            sizeStats.setP95(0.0);
            stats.setResponseSizeInBytes(sizeStats);

            stats.setStartDate(fromDate);
            stats.setEndDate(toDate);

            return stats;
        }

        List<Long> responseSizes =
                allEntries.stream().map(LogEntry::getResponseSizeBytes).collect(Collectors.toList());

        double avg = responseSizes.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double max = responseSizes.stream().mapToLong(Long::longValue).max().orElse(0L);
        double p95 = Percentile.p95(responseSizes);

        LogStatistics.ResponseSize sizeStats = new LogStatistics.ResponseSize();
        sizeStats.setAverage(round(avg));
        sizeStats.setMax(round(max));
        sizeStats.setP95(round(p95));
        stats.setResponseSizeInBytes(sizeStats);

        Map<String, Long> resourceCounts =
                allEntries.stream().collect(Collectors.groupingBy(LogEntry::getResource, Collectors.counting()));
        List<LogStatistics.ResourceCount> topResources = resourceCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_RESOURCES_LIMIT)
                .map(e -> {
                    LogStatistics.ResourceCount rc = new LogStatistics.ResourceCount();
                    rc.setResource(e.getKey());
                    rc.setTotalRequestsCount(e.getValue().intValue());
                    return rc;
                })
                .collect(Collectors.toList());
        stats.setResources(topResources);

        Map<Integer, Long> codeCounts =
                allEntries.stream().collect(Collectors.groupingBy(LogEntry::getStatusCode, Collectors.counting()));
        List<LogStatistics.ResponseCodeCount> codeStats = codeCounts.entrySet().stream()
                .map(e -> {
                    LogStatistics.ResponseCodeCount c = new LogStatistics.ResponseCodeCount();
                    c.setCode(e.getKey());
                    c.setTotalResponsesCount(e.getValue().intValue());
                    return c;
                })
                .collect(Collectors.toList());
        stats.setResponseCodes(codeStats);

        List<String> uniqueProtocols =
                allEntries.stream().map(LogEntry::getProtocol).distinct().collect(Collectors.toList());
        stats.setUniqueProtocols(uniqueProtocols);

        Map<LocalDate, Long> dateCounts = allEntries.stream()
                .collect(Collectors.groupingBy(e -> e.getTimestamp().toLocalDate(), Collectors.counting()));

        LocalDate actualStartDate =
                dateCounts.keySet().stream().min(LocalDate::compareTo).orElse(null);
        LocalDate actualEndDate =
                dateCounts.keySet().stream().max(LocalDate::compareTo).orElse(null);

        stats.setStartDate(actualStartDate);
        stats.setEndDate(actualEndDate);

        List<LogStatistics.RequestsPerDate> dateStats = dateCounts.entrySet().stream()
                .map(e -> {
                    LocalDate date = e.getKey();
                    long count = e.getValue();
                    double percentage = (double) count / totalCount * 100;
                    LogStatistics.RequestsPerDate dpd = new LogStatistics.RequestsPerDate();
                    dpd.setDate(date.format(DATE_FORMATTER));
                    dpd.setWeekday(date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault()));
                    dpd.setTotalRequestsCount((int) count);
                    dpd.setTotalRequestsPercentage(round(percentage));
                    return dpd;
                })
                .collect(Collectors.toList());
        stats.setRequestsPerDate(dateStats);

        logger.info("Обработка завершена. Всего записей: {}", totalCount);
        return stats;
    }

    private boolean isWithinDateRange(LocalDate entryDate) {
        if (fromDate != null && entryDate.isBefore(fromDate)) return false;
        if (toDate != null && entryDate.isAfter(toDate)) return false;
        return true;
    }

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
