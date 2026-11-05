package client;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import model.AuthData;
import request.LoginRequest;
import request.RegisterRequest;
import result.LoginResult;
import result.RegisterResult;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/** Sends client requests to the HTTP server without exposing transport details to the UI. */
public class ServerFacade {
    private static final int TIMEOUT_MILLIS = 5000;
    private final URI serverUri;
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("The server port must be between 1 and 65535.");
        }
        serverUri = URI.create("http://localhost:" + port);
    }

    /** Clears application data for test setup; this is not a player command. */
    public void clear() throws ServerException {
        send("DELETE", "/db", null);
    }

    public RegisterResult register(String username, String password, String email) throws ServerException {
        var auth = authenticate("/user", new RegisterRequest(username, password, email), username, "registration");
        return new RegisterResult(auth.username(), auth.authToken());
    }

    public LoginResult login(String username, String password) throws ServerException {
        var auth = authenticate("/session", new LoginRequest(username, password), username, "login");
        return new LoginResult(auth.username(), auth.authToken());
    }

    private AuthData authenticate(String path, Object request, String username, String operation) throws ServerException {
        String json = send("POST", path, request);
        String invalidResponse = "The server returned an invalid " + operation + " response. Please try again.";
        try {
            var auth = gson.fromJson(json, AuthData.class);
            if (auth == null || auth.username() == null || !auth.username().equals(username)
                    || auth.authToken() == null || auth.authToken().isBlank()) {
                throw new ServerException(invalidResponse);
            }
            return auth;
        } catch (JsonParseException exception) {
            throw new ServerException(invalidResponse, exception);
        }
    }

    private String send(String method, String path, Object body) throws ServerException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) serverUri.resolve(path).toURL().openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            if (body != null) {
                writeBody(connection, body);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw responseError(status);
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

    private void writeBody(HttpURLConnection connection, Object body) throws IOException {
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        byte[] json = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        connection.setFixedLengthStreamingMode(json.length);
        try (var output = connection.getOutputStream()) {
            output.write(json);
        }
    }

    private ServerException responseError(int status) {
        String message = switch (status) {
            case 400 -> "Please check the information you entered.";
            case 401 -> "Incorrect username or password.";
            case 403 -> "That username is already taken.";
            default -> "The server could not complete the request. Please try again.";
        };
        return new ServerException(message);
    }

}
