package client;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import model.AuthData;
import request.CreateGameRequest;
import request.LoginRequest;
import request.RegisterRequest;
import result.CreateGameResult;
import result.LoginResult;
import result.ListGamesResult;
import result.RegisterResult;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Sends client requests to the HTTP server without exposing transport details to the UI. */
public class ServerFacade {
    private static final int TIMEOUT_MILLIS = 5000;
    private static final String INVALID_SESSION = "Your session is no longer valid. Please log in again.";
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
        send("DELETE", "/db", null, null);
    }

    public RegisterResult register(String username, String password, String email) throws ServerException {
        var auth = authenticate("/user", new RegisterRequest(username, password, email), username, "registration");
        return new RegisterResult(auth.username(), auth.authToken());
    }

    public LoginResult login(String username, String password) throws ServerException {
        var auth = authenticate("/session", new LoginRequest(username, password), username, "login");
        return new LoginResult(auth.username(), auth.authToken());
    }

    public void logout(String authToken) throws ServerException {
        requireToken(authToken);
        send("DELETE", "/session", null, authToken);
    }

    public CreateGameResult createGame(String authToken, String gameName) throws ServerException {
        requireToken(authToken);
        // Authorization belongs in the header, not in the JSON body.
        var request = new CreateGameRequest(null, gameName);
        String json = send("POST", "/game", request, authToken);
        String invalidResponse = "The server returned an invalid create-game response. Please try again.";
        var result = readResponse(json, CreateGameResult.class, invalidResponse);
        if (result.gameID() <= 0) {
            throw new ServerException(invalidResponse);
        }
        return result;
    }

    public ListGamesResult listGames(String authToken) throws ServerException {
        requireToken(authToken);
        String json = send("GET", "/game", null, authToken);
        String invalidResponse = "The server returned an invalid game-list response. Please try again.";
        var result = readResponse(json, ListGamesResult.class, invalidResponse);
        if (result.games() == null) {
            throw new ServerException(invalidResponse);
        }
        for (var game : result.games()) {
            if (game == null || game.gameID() <= 0 || game.gameName() == null || game.gameName().isBlank()) {
                throw new ServerException(invalidResponse);
            }
        }
        return new ListGamesResult(List.copyOf(result.games()));
    }

    private void requireToken(String authToken) throws ServerException {
        if (authToken == null || authToken.isBlank() || authToken.contains("\r") || authToken.contains("\n")) {
            throw new ServerException(INVALID_SESSION);
        }
    }

    private AuthData authenticate(String path, Object request, String username, String operation) throws ServerException {
        String json = send("POST", path, request, null);
        String invalidResponse = "The server returned an invalid " + operation + " response. Please try again.";
        var auth = readResponse(json, AuthData.class, invalidResponse);
        if (auth.username() == null || !auth.username().equals(username)
                || auth.authToken() == null || auth.authToken().isBlank()) {
            throw new ServerException(invalidResponse);
        }
        return auth;
    }

    private <T> T readResponse(String json, Class<T> type, String invalidResponse) throws ServerException {
        try {
            var result = gson.fromJson(json, type);
            if (result == null) {
                throw new ServerException(invalidResponse);
            }
            return result;
        } catch (JsonParseException exception) {
            throw new ServerException(invalidResponse, exception);
        }
    }

    private String send(String method, String path, Object body, String authToken) throws ServerException {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) serverUri.resolve(path).toURL().openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(TIMEOUT_MILLIS);
            connection.setReadTimeout(TIMEOUT_MILLIS);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Accept", "application/json");
            if (authToken != null) {
                connection.setRequestProperty("authorization", authToken);
            }
            if (body != null) {
                writeBody(connection, body);
            }
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) {
                throw responseError(status, method, path);
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

    private ServerException responseError(int status, String method, String path) {
        String message = switch (status) {
            case 400 -> "Please check the information you entered.";
            case 401 -> method.equals("POST") && path.equals("/session")
                    ? "Incorrect username or password." : INVALID_SESSION;
            case 403 -> "That username is already taken.";
            default -> "The server could not complete the request. Please try again.";
        };
        return new ServerException(message);
    }

}
