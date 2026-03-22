package client;

import com.google.gson.Gson;

import java.io.*;
import java.net.*;

public class ServerFacade {

    private final String serverUrl;
    private final Gson gson = new Gson();

    public ServerFacade(int port) {
        this.serverUrl = "http://localhost:" + port;
    }

    public record AuthResult(String username, String authToken) { }
    public record GameInfo(int gameID, String whiteUsername, String blackUsername, String gameName) { }
    private record ListGamesResponse(GameInfo[] games) { }
    private record CreateGameResponse(int gameID) { }
    private record ErrorResponse(String message) { }

    public AuthResult register(String username, String password, String email) throws ServerFacadeException {
        record RegisterRequest(String username, String password, String email) { }
        return makeRequest("POST", "/user", new RegisterRequest(username, password, email), null, AuthResult.class);
    }

    public AuthResult login(String username, String password) throws ServerFacadeException {
        record LoginRequest(String username, String password) { }
        return makeRequest("POST", "/session", new LoginRequest(username, password), null, AuthResult.class);
    }

    public void logout(String authToken) throws ServerFacadeException {
        makeRequest("DELETE", "/session", null, authToken, null);
    }

    public GameInfo[] listGames(String authToken) throws ServerFacadeException {
        var response = makeRequest("GET", "/game", null, authToken, ListGamesResponse.class);
        return response.games() != null ? response.games() : new GameInfo[0];
    }

    public int createGame(String authToken, String gameName) throws ServerFacadeException {
        record CreateGameRequest(String gameName) { }
        var response = makeRequest("POST", "/game", new CreateGameRequest(gameName), authToken, CreateGameResponse.class);
        return response.gameID();
    }

    public void joinGame(String authToken, String playerColor, int gameID) throws ServerFacadeException {
        record JoinGameRequest(String playerColor, int gameID) { }
        makeRequest("PUT", "/game", new JoinGameRequest(playerColor, gameID), authToken, null);
    }

    public void clear() throws ServerFacadeException {
        makeRequest("DELETE", "/db", null, null, null);
    }

    private <T> T makeRequest(String method, String path, Object requestBody, String authToken, Class<T> responseClass)
            throws ServerFacadeException {
        try {
            URL url = new URI(serverUrl + path).toURL();
            HttpURLConnection http = (HttpURLConnection) url.openConnection();
            http.setRequestMethod(method);
            http.setRequestProperty("Accept", "application/json");

            if (authToken != null) {
                http.setRequestProperty("Authorization", authToken);
            }

            if (requestBody != null) {
                http.setDoOutput(true);
                http.setRequestProperty("Content-Type", "application/json");
                try (OutputStream os = http.getOutputStream()) {
                    os.write(gson.toJson(requestBody).getBytes());
                }
            }

            int status = http.getResponseCode();
            if (status >= 400) {
                String errorMessage = readBody(http.getErrorStream());
                try {
                    var errorResponse = gson.fromJson(errorMessage, ErrorResponse.class);
                    if (errorResponse != null && errorResponse.message() != null) {
                        throw new ServerFacadeException(status, errorResponse.message());
                    }
                } catch (com.google.gson.JsonSyntaxException ignored) {
                }
                throw new ServerFacadeException(status, "HTTP " + status);
            }

            if (responseClass != null) {
                String body = readBody(http.getInputStream());
                return gson.fromJson(body, responseClass);
            }
            return null;
        } catch (ServerFacadeException e) {
            throw e;
        } catch (ConnectException e) {
            throw new ServerFacadeException(0, "Cannot connect to server. Is it running?");
        } catch (Exception e) {
            throw new ServerFacadeException(500, e.getMessage());
        }
    }

    private String readBody(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        try (var reader = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }
}
