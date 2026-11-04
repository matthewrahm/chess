package client;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Sends client requests to the HTTP server without exposing transport details to the UI. */
public class ServerFacade {
    private static final int TIMEOUT_MILLIS = 5000;
    private final URI serverUri;

    public ServerFacade(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("The server port must be between 1 and 65535.");
        }
        serverUri = URI.create("http://localhost:" + port);
    }

    /** Clears application data for test setup; this is not a player command. */
    public void clear() throws ServerException {
        send("DELETE", "/db");
    }

    private String send(String method, String path) throws ServerException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) serverUri.resolve(path).toURL().openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw new ServerException("The server could not complete the request. Please try again.");
            }
            try (var input = connection.getInputStream()) {
                return new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException exception) {
            throw new ServerException("Unable to reach the server. Please try again.", exception);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
