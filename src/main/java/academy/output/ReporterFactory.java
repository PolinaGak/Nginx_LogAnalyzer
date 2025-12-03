package academy.output;

public class ReporterFactory {
    public static Reporter create(String format) {
        return switch (format.toLowerCase()) {
            case "json" -> new JsonReporter();
            case "markdown" -> new MarkdownReporter();
            case "adoc" -> new AdocReporter();
            default -> throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
        };
    }
}
