package academy.io;

import java.util.stream.Stream;

public interface LogSource extends AutoCloseable {
    Stream<String> lines();

    String getSourceIdentifier();

    @Override
    void close();
}
