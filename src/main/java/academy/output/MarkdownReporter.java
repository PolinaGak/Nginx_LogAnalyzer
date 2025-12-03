package academy.output;

import academy.stats.LogStatistics;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class MarkdownReporter implements Reporter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.US);
    private static final String LN = "%n";

    @Override
    public void write(LogStatistics stats, Path outputPath) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {

            writer.printf("#### Общая информация%s%s", LN, LN);

            writer.printf("|        Метрика        |     Значение |%s", LN);
            writer.printf("|:---------------------:|-------------:|%s", LN);
            writer.printf("|       Файл(-ы)        | `%s` |%s", String.join(", ", stats.getFiles()), LN);
            writer.printf("|    Начальная дата     | %s |%s", formatDate(stats.getStartDate()), LN);
            writer.printf("|     Конечная дата     | %s |%s", formatDate(stats.getEndDate()), LN);
            writer.printf("|  Количество запросов  |       %,d |%s", stats.getTotalRequestsCount(), LN);
            writer.printf(
                    "| Средний размер ответа |      %,.2f b |%s",
                    stats.getResponseSizeInBytes().getAverage(), LN);
            writer.printf(
                    "|  95p размера ответа   |      %,.2f b |%s",
                    stats.getResponseSizeInBytes().getP95(), LN);

            // Топ ресурсов
            writer.printf("%s#### Запрашиваемые ресурсы%s%s", LN, LN, LN);
            writer.printf("|     Ресурс      | Количество |%s", LN);
            writer.printf("|:---------------:|-----------:|%s", LN);
            for (LogStatistics.ResourceCount rc : stats.getResources()) {
                writer.printf("|  %-20s | %,11d |%s", "`" + rc.getResource() + "`", rc.getTotalRequestsCount(), LN);
            }

            // Коды ответа
            writer.printf("%s#### Коды ответа%s%s", LN, LN, LN);
            writer.printf("| Код |          Имя          | Количество |%s", LN);
            writer.printf("|:---:|:---------------------:|-----------:|%s", LN);
            List<LogStatistics.ResponseCodeCount> codes = stats.getResponseCodes();
            codes.sort((a, b) -> Integer.compare(b.getTotalResponsesCount(), a.getTotalResponsesCount()));
            for (LogStatistics.ResponseCodeCount c : codes) {
                String name = statusCodeToName(c.getCode());
                writer.printf("| %3d | %-21s | %,11d |%s", c.getCode(), name, c.getTotalResponsesCount(), LN);
            }

            // Распределение по датам
            writer.printf("%s#### Распределение запросов по датам%s%s", LN, LN, LN);
            writer.printf("|     Дата     |  День недели  | Количество | Процент |%s", LN);
            writer.printf("|:------------:|:-------------:|:----------:|:-------:|%s", LN);
            for (LogStatistics.RequestsPerDate d : stats.getRequestsPerDate()) {
                writer.printf(
                        "| %-12s | %-13s | %,11d | %7.2f%% |%s",
                        d.getDate(), d.getWeekday(), d.getTotalRequestsCount(), d.getTotalRequestsPercentage(), LN);
            }

            // Уникальные протоколы
            if (stats.getUniqueProtocols() != null
                    && !stats.getUniqueProtocols().isEmpty()) {
                writer.printf("%s#### Уникальные протоколы%s%s", LN, LN, LN);
                writer.printf("`%s`%s", String.join("`, `", stats.getUniqueProtocols()), LN);
            }

        } catch (IOException e) {
            throw new RuntimeException("Не удалось записать Markdown-отчёт: " + e.getMessage(), e);
        }
    }

    private String formatDate(LocalDate date) {
        return date == null ? "-" : date.format(DATE_FORMAT);
    }

    private String statusCodeToName(int code) {
        return HttpStatus.getNameByCode(code);
    }
}
