package academy.io;

import java.net.URI;

public class LogSourceFactory {

    public static LogSource from(String pathOrUrl) {
        if (isUrl(pathOrUrl)) {
            return new RemoteLogSource(pathOrUrl);
        } else {
            return new LocalLogSource(pathOrUrl);
        }
    }

    private static boolean isUrl(String s) {
        try {
            URI uri = URI.create(s);
            return uri.getScheme() != null
                    && (uri.getScheme().startsWith("http") || uri.getScheme().startsWith("https"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
