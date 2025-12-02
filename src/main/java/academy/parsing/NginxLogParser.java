package academy.parsing;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class NginxLogParser {
    private static final Logger logger = LogManager.getLogger(NginxLogParser.class);

    private static final Pattern LOG_PATTERN =
            Pattern.compile("^([\\d.]+) - - \\[(.*?)\\] \"(.*?)\" (\\d{3}) " + "(\\d+|-) \"(.*?)\" \"(.*?)\"\\s*$");

    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z", Locale.US);

    public Optional<LogEntry> parse(String line) {
        if (line == null || line.isEmpty()) {
            logger.debug("Пропуск пустой строки");
            return Optional.empty();
        }

        String trimmedLine = line.trim();
        logger.trace("Парсинг строки: '{}'", trimmedLine);

        Matcher matcher = LOG_PATTERN.matcher(trimmedLine);
        if (!matcher.matches()) {
            logger.debug("Строка не соответствует формату NGINX: '{}'", trimmedLine);
            return Optional.empty();
        }

        try {
            // Группы из регулярного выражения:
            // 1: IP адрес
            // 2: Временная метка
            // 3: HTTP запрос
            // 4: Код состояния
            // 5: Размер ответа
            // 6: Referer
            // 7: User-Agent

            String remoteAddr = matcher.group(1);
            String timeStr = matcher.group(2);
            String requestLine = matcher.group(3);
            int statusCode = Integer.parseInt(matcher.group(4));
            String bodySizeStr = matcher.group(5);
            String referer = matcher.group(6);
            String userAgent = matcher.group(7);

            long responseSizeBytes = "-".equals(bodySizeStr) ? 0L : Long.parseLong(bodySizeStr);

            LocalDateTime timestamp = parseTimestamp(timeStr);

            String[] requestParts = requestLine.split(" ", 3);
            if (requestParts.length < 3) {
                logger.debug("Неверный формат HTTP запроса: '{}'", requestLine);
                return Optional.empty();
            }

            String resource = requestParts[1];
            String protocol = requestParts[2];

            LogEntry entry = new LogEntry(
                    timestamp, remoteAddr, statusCode, responseSizeBytes, resource, protocol, referer, userAgent);

            logger.trace("Успешно распарсена запись: {}", entry);
            return Optional.of(entry);

        } catch (Exception e) {
            logger.warn("Ошибка при парсинге строки: {}", trimmedLine, e);
            return Optional.empty();
        }
    }

    private LocalDateTime parseTimestamp(String timeStr) {
        try {
            ZonedDateTime zdt = ZonedDateTime.parse(timeStr, TIME_FORMATTER);
            return zdt.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime();
        } catch (DateTimeParseException e) {
            logger.error("Ошибка парсинга времени '{}' из строки", timeStr, e);
            throw e;
        }
    }
}
