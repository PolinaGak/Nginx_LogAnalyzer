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

    @Override
    public void write(LogStatistics stats, Path outputPath) {
        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outputPath))) {
            writer.println("#### Общая информация\n");

            writer.println("|        Метрика        |     Значение |");
            writer.println("|:---------------------:|-------------:|");
            writer.printf("|       Файл(-ы)        | `%s` |\n", String.join(", ", stats.getFiles()));
            writer.printf("|    Начальная дата     | %s |\n", formatDate(stats.getStartDate()));
            writer.printf("|     Конечная дата     | %s |\n", formatDate(stats.getEndDate()));
            writer.printf("|  Количество запросов  |       %,d |\n", stats.getTotalRequestsCount());
            writer.printf(
                    "| Средний размер ответа |      %,.2fb |\n",
                    stats.getResponseSizeInBytes().getAverage());
            writer.printf(
                    "|  95p размера ответа   |      %,.2fb |\n",
                    stats.getResponseSizeInBytes().getP95());

            // Топ ресурсов
            writer.println("\n#### Запрашиваемые ресурсы\n");
            writer.println("|     Ресурс      | Количество |");
            writer.println("|:---------------:|-----------:|");
            for (LogStatistics.ResourceCount rc : stats.getResources()) {
                writer.printf("|  %-20s | %,11d |\n", "`" + rc.getResource() + "`", rc.getTotalRequestsCount());
            }

            // Коды ответа
            writer.println("\n#### Коды ответа\n");
            writer.println("| Код |          Имя          | Количество |");
            writer.println("|:---:|:---------------------:|-----------:|");
            List<LogStatistics.ResponseCodeCount> codes = stats.getResponseCodes();
            codes.sort((a, b) -> Integer.compare(b.getTotalResponsesCount(), a.getTotalResponsesCount()));
            for (LogStatistics.ResponseCodeCount c : codes) {
                String name = statusCodeToName(c.getCode());
                writer.printf("| %3d | %-21s | %,11d |\n", c.getCode(), name, c.getTotalResponsesCount());
            }

            // Распределение по датам
            writer.println("\n#### Распределение запросов по датам\n");
            writer.println("|     Дата     |  День недели  | Количество | Процент |");
            writer.println("|:------------:|:-------------:|:----------:|:-------:|");
            for (LogStatistics.RequestsPerDate d : stats.getRequestsPerDate()) {
                writer.printf(
                        "| %-12s | %-13s | %,11d | %7.2f%% |\n",
                        d.getDate(), d.getWeekday(), d.getTotalRequestsCount(), d.getTotalRequestsPercentage());
            }

            // Уникальные протоколы
            if (stats.getUniqueProtocols() != null
                    && !stats.getUniqueProtocols().isEmpty()) {
                writer.println("\n#### Уникальные протоколы\n");
                writer.println("`" + String.join("`, `", stats.getUniqueProtocols()) + "`");
            }

        } catch (IOException e) {
            throw new RuntimeException("Не удалось записать Markdown-отчёт: " + e.getMessage(), e);
        }
    }

    private String formatDate(LocalDate date) {
        if (date == null) return "-";
        return date.format(DATE_FORMAT);
    }

    private String statusCodeToName(int code) {
        return HttpStatus.getNameByCode(code);
    }
}
