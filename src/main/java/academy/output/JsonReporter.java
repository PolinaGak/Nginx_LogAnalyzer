package academy.output;

import academy.stats.LogStatistics;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Path;

public class JsonReporter implements Reporter {

    private final ObjectMapper mapper;

    public JsonReporter() {
        this.mapper = JsonMapper.builder()
                .addModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .enable(SerializationFeature.INDENT_OUTPUT)
                .build();
    }

    @Override
    public void write(LogStatistics stats, Path outputPath) {
        try {
            mapper.writeValue(outputPath.toFile(), stats);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось записать JSON-отчёт: " + e.getMessage(), e);
        }
    }
}
