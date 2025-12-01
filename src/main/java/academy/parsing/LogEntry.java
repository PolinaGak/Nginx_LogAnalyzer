package academy.parsing;

import java.time.LocalDateTime;

public class LogEntry {
  private final LocalDateTime timestamp;
  private final String remoteAddr;
  private final int statusCode;
  private final long responseSizeBytes;
  private final String resource;
  private final String protocol;
  private final String userAgent;
  private final String referer;

  public LogEntry(LocalDateTime timestamp, String remoteAddr, int statusCode,
                  long responseSizeBytes, String resource, String protocol,
                  String referer, String userAgent) {
    this.timestamp = timestamp;
    this.remoteAddr = remoteAddr;
    this.statusCode = statusCode;
    this.responseSizeBytes = responseSizeBytes;
    this.resource = resource;
    this.protocol = protocol;
    this.referer = referer;
    this.userAgent = userAgent;
  }
  public LocalDateTime getTimestamp() { return timestamp; }

  public String getRemoteAddr() { return remoteAddr; }

  public int getStatusCode() { return statusCode; }

  public long getResponseSizeBytes() { return responseSizeBytes; }

  public String getResource() { return resource; }

  public String getProtocol() { return protocol; }

  public String getUserAgent() { return userAgent; }

  public String getReferer() { return referer; }

  @Override
  public String toString() {
    return "LogEntry{"
        + "timestamp=" + timestamp + ", statusCode=" + statusCode +
        ", resource='" + resource + '\'' + '}';
  }
}