package academy.output;

import academy.stats.LogStatistics;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

public class AdocReporter implements Reporter {
  @Override
  public void write(LogStatistics stats, Path outputPath) {
    try (PrintWriter writer =
             new PrintWriter(Files.newBufferedWriter(outputPath))) {
      writer.println("= Отчёт по логам\n");

      writer.println("== Общая информация\n");
      writer.printf("Файлы:: `%s`%n", String.join(", ", stats.getFiles()));
      writer.printf("Начальная дата:: %s%n", stats.getStartDate());
      writer.printf("Конечная дата:: %s%n", stats.getEndDate());
      writer.printf("Количество запросов:: %,d%n",
                    stats.getTotalRequestsCount());
      writer.printf("Средний размер ответа:: %,.2f b%n",
                    stats.getResponseSizeInBytes().getAverage());
      writer.printf("95-й перцентиль:: %,.2f b%n",
                    stats.getResponseSizeInBytes().getP95());

      writer.println("\n== Топ ресурсов\n");
      writer.println("[options=\"header\", cols=\"2,1\"]");
      writer.println("|===");
      writer.println("| Ресурс | Запросов");
      for (LogStatistics.ResourceCount rc : stats.getResources()) {
        writer.printf("| `%s` | %,d%n", rc.getResource(),
                      rc.getTotalRequestsCount());
      }
      writer.println("|===");

      writer.println("\n== Коды ответа\n");
      writer.println("[options=\"header\", cols=\"1,2,1\"]");
      writer.println("|===");
      writer.println("| Код | Название | Количество");
      for (LogStatistics.ResponseCodeCount c : stats.getResponseCodes()) {
        writer.printf("| %d | %s | %,d%n", c.getCode(),
                      statusCodeToName(c.getCode()),
                      c.getTotalResponsesCount());
      }
      writer.println("|===");

      if (stats.getRequestsPerDate() != null &&
          !stats.getRequestsPerDate().isEmpty()) {
        writer.println("\n== Распределение по датам\n");
        writer.println("[options=\"header\"]");
        writer.println("|===");
        writer.println("| Дата | День | Запросов | Процент");
        for (LogStatistics.RequestsPerDate d : stats.getRequestsPerDate()) {
          writer.printf("| %s | %s | %,d | %.2f%%%n", d.getDate(),
                        d.getWeekday(), d.getTotalRequestsCount(),
                        d.getTotalRequestsPercentage());
        }
        writer.println("|===");
      }

      if (stats.getUniqueProtocols() != null &&
          !stats.getUniqueProtocols().isEmpty()) {
        writer.println("\n== Уникальные протоколы\n");
        writer.print("Протоколы: ");
        writer.println(String.join(", ", stats.getUniqueProtocols()));
      }

    } catch (IOException e) {
      throw new RuntimeException(
          "Не удалось записать AsciiDoc-отчёт: " + e.getMessage(), e);
    }
  }

  private String statusCodeToName(int code) {
    return HttpStatus.getNameByCode(code);
  }
}