package academy.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RemoteLogSource implements LogSource {
    private static final Logger logger = LogManager.getLogger(RemoteLogSource.class);

    private final String url;
    private BufferedReader reader;
    private boolean alreadyRead = false;

    public RemoteLogSource(String url) {
        this.url = url;
    }

    @Override
    public Stream<String> lines() {
        if (alreadyRead) {
            throw new IllegalStateException("Stream уже был прочитан");
        }
        alreadyRead = true;

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0 (log-analyzer)")
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("Ошибка загрузки " + url + ": " + response.statusCode());
            }

            reader = new BufferedReader(new StringReader(response.body()));
            return reader.lines().onClose(this::close);

        } catch (IOException | InterruptedException e) {
            logger.error("Ошибка при чтении удалённого лога {}: {}", url, e.getMessage());
            close();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getSourceIdentifier() {
        return url;
    }

    @Override
    public void close() {
        if (reader != null) {
            try {
                reader.close();
            } catch (IOException ignored) {
            }
            reader = null;
        }
    }
}
