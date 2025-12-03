package academy.output;

import academy.stats.LogStatistics;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class JsonReporter implements Reporter {

    private static final DateTimeFormatter OUTPUT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter INPUT_DATE_PARSER = DateTimeFormatter.ofPattern("yyyy-MM-d");

    @JsonPropertyOrder({
        "files", "totalRequestsCount", "responseSizeInBytes",
        "resources", "responseCodes", "requestsPerDate",
        "uniqueProtocols"
    })
    private static class ReportDto {
        public List<String> files;
        public int totalRequestsCount;
        public ResponseSizeDto responseSizeInBytes;
        public List<ResourceDto> resources;
        public List<ResponseCodeDto> responseCodes;
        public List<RequestsPerDateDto> requestsPerDate;
        public List<String> uniqueProtocols;

        public static class ResponseSizeDto {
            public double average;
            public double max;
            public double p95;
        }

        public static class ResourceDto {
            public String resource;
            public int totalRequestsCount;
        }

        public static class ResponseCodeDto {
            public int code;
            public int totalResponsesCount;
        }

        public static class RequestsPerDateDto {
            public String date;
            public String weekday;
            public int totalRequestsCount;
            public double totalRequestsPercentage;
        }
    }

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
            ReportDto dto = convertToDto(stats);
            mapper.writeValue(outputPath.toFile(), dto);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось записать JSON-отчёт: " + e.getMessage(), e);
        }
    }

    private ReportDto convertToDto(LogStatistics stats) {
        ReportDto dto = new ReportDto();

        dto.files = stats.getFiles();
        dto.totalRequestsCount = stats.getTotalRequestsCount();

        if (stats.getResponseSizeInBytes() != null) {
            ReportDto.ResponseSizeDto size = new ReportDto.ResponseSizeDto();
            size.average = stats.getResponseSizeInBytes().getAverage();
            size.max = stats.getResponseSizeInBytes().getMax();
            size.p95 = stats.getResponseSizeInBytes().getP95();
            dto.responseSizeInBytes = size;
        }

        dto.resources = stats.getResources().stream()
                .map(r -> {
                    ReportDto.ResourceDto d = new ReportDto.ResourceDto();
                    d.resource = r.getResource();
                    d.totalRequestsCount = r.getTotalRequestsCount();
                    return d;
                })
                .collect(Collectors.toList());

        dto.responseCodes = stats.getResponseCodes().stream()
                .map(c -> {
                    ReportDto.ResponseCodeDto d = new ReportDto.ResponseCodeDto();
                    d.code = c.getCode();
                    d.totalResponsesCount = c.getTotalResponsesCount();
                    return d;
                })
                .collect(Collectors.toList());

        dto.requestsPerDate = stats.getRequestsPerDate().stream()
                .map(r -> {
                    ReportDto.RequestsPerDateDto d = new ReportDto.RequestsPerDateDto();
                    try {
                        LocalDate parsed = LocalDate.parse(r.getDate(), INPUT_DATE_PARSER);
                        d.date = parsed.format(OUTPUT_DATE_FORMAT);
                    } catch (Exception e) {
                        d.date = r.getDate();
                    }
                    d.weekday = r.getWeekday();
                    d.totalRequestsCount = r.getTotalRequestsCount();
                    d.totalRequestsPercentage = r.getTotalRequestsPercentage();
                    return d;
                })
                .collect(Collectors.toList());

        List<String> desiredOrder = List.of("HTTP/1.1", "HTTP/1.0", "HTTP/2.1", "grpc");
        dto.uniqueProtocols = desiredOrder.stream()
                .filter(stats.getUniqueProtocols()::contains)
                .collect(Collectors.toList());

        for (String proto : stats.getUniqueProtocols()) {
            if (!dto.uniqueProtocols.contains(proto)) {
                dto.uniqueProtocols.add(proto);
            }
        }

        return dto;
    }
}
