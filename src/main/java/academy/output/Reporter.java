package academy.output;

import academy.stats.LogStatistics;
import java.nio.file.Path;

public interface Reporter {
  void write(LogStatistics stats, Path outputPath);
}