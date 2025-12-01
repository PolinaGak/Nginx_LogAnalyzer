package academy.output;

public enum HttpStatus {
  OK(200, "OK"),

  MOVED_PERMANENTLY(301, "Moved Permanently"),
  FOUND(302, "Found"),
  NOT_MODIFIED(304, "Not Modified"),

  BAD_REQUEST(400, "Bad Request"),
  UNAUTHORIZED(401, "Unauthorized"),
  FORBIDDEN(403, "Forbidden"),
  NOT_FOUND(404, "Not Found"),

  INTERNAL_SERVER_ERROR(500, "Internal Server Error"),
  BAD_GATEWAY(502, "Bad Gateway"),
  SERVICE_UNAVAILABLE(503, "Service Unavailable");

  private final int code;
  private final String name;

  HttpStatus(int code, String name) {
    this.code = code;
    this.name = name;
  }

  public int getCode() { return code; }

  public String getName() { return name; }

  public static HttpStatus fromCode(int code) {
    for (HttpStatus status : values()) {
      if (status.code == code) {
        return status;
      }
    }
    return null;
  }

  public static String getNameByCode(int code) {
    HttpStatus status = fromCode(code);
    if (status != null) {
      return status.getName();
    }

    return switch (code / 100) {
      case 2 -> "OK";
      case 3 -> "Redirect";
      case 4 -> "Client Error";
      case 5 -> "Server Error";
      default -> "Unknown";
    };
  }
}