package handler;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mindrot.jbcrypt.BCrypt;
import server.Server;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class UserHttpTests {
    private static final String REGISTER = """
            {"username":"alice","password":"secret","email":"alice@example.com"}
            """;
    private static final String LOGIN = """
            {"username":"alice","password":"secret"}
            """;
    private MemoryDataAccess data;
    private Server server;
    private HttpClient client;
    private int port;

    @BeforeEach
    void setUp() {
        data = new MemoryDataAccess();
        server = new Server(data);
        port = server.run(0);
        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    @AfterEach
    void tearDown() {
        client.close();
        server.stop();
    }

    private HttpResponse<String> post(String path, String body) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(5)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonObject json(HttpResponse<String> response) {
        assertTrue(response.headers().firstValue("content-type").orElse("").startsWith("application/json"));
        return JsonParser.parseString(response.body()).getAsJsonObject();
    }

    private void assertError(HttpResponse<String> response, int status, String message) {
        assertEquals(status, response.statusCode(), response.body());
        var result = json(response);
        assertEquals(Set.of("message"), result.keySet());
        assertEquals(message, result.get("message").getAsString());
    }

    @Test
    void registerAndLoginReturnIndependentStoredTokensWithoutPrivateFields() throws Exception {
        var registration = post("/user", REGISTER);
        assertEquals(200, registration.statusCode());
        var registered = json(registration);
        assertEquals(Set.of("username", "authToken"), registered.keySet());
        assertEquals("alice", registered.get("username").getAsString());
        String originalToken = registered.get("authToken").getAsString();
        assertDoesNotThrow(() -> UUID.fromString(originalToken));
        assertEquals(new AuthData(originalToken, "alice"), data.getAuth(originalToken));
        var user = data.getUser("alice");
        assertEquals("alice@example.com", user.email());
        assertNotEquals("secret", user.password());
        assertTrue(BCrypt.checkpw("secret", user.password()));

        var first = post("/session", LOGIN);
        var second = post("/session", LOGIN);
        assertEquals(200, first.statusCode());
        assertEquals(200, second.statusCode());
        var firstLogin = json(first);
        var secondLogin = json(second);
        assertEquals(Set.of("username", "authToken"), firstLogin.keySet());
        assertEquals(Set.of("username", "authToken"), secondLogin.keySet());
        String firstToken = firstLogin.get("authToken").getAsString();
        String secondToken = secondLogin.get("authToken").getAsString();
        assertEquals(3, Set.of(originalToken, firstToken, secondToken).size());
        assertEquals("alice", firstLogin.get("username").getAsString());
        assertEquals(new AuthData(firstToken, "alice"), data.getAuth(firstToken));
        assertEquals(new AuthData(secondToken, "alice"), data.getAuth(secondToken));
        assertNotNull(data.getAuth(originalToken));
    }

    @Test
    void duplicateRegistrationPreservesOriginalAccountAndToken() throws Exception {
        String token = json(post("/user", REGISTER)).get("authToken").getAsString();
        var stored = data.getUser("alice");
        assertError(post("/user", REGISTER.replace("secret", "replacement")), 403, "Error: already taken");
        assertEquals(stored, data.getUser("alice"));
        assertNotNull(data.getAuth(token));
        assertEquals(200, post("/session", LOGIN).statusCode());
        assertError(post("/session", LOGIN.replace("secret", "replacement")), 401, "Error: unauthorized");
    }

    @Test
    void unknownUserAndWrongPasswordHaveSameUnauthorizedResponse() throws Exception {
        post("/user", REGISTER);
        var unknown = post("/session", LOGIN.replace("alice", "nobody"));
        var wrong = post("/session", LOGIN.replace("secret", "wrong"));
        assertError(unknown, 401, "Error: unauthorized");
        assertError(wrong, 401, "Error: unauthorized");
        assertEquals(unknown.body(), wrong.body());
    }

    @ParameterizedTest
    @MethodSource("invalidJson")
    void invalidJsonOrFieldTypesAreBadRequestsOnBothRoutes(String body) throws Exception {
        assertError(post("/user", body), 400, "Error: bad request");
        assertError(post("/session", body), 400, "Error: bad request");
        assertNull(data.getUser("alice"));
    }

    static Stream<String> invalidJson() {
        return Stream.of("", "{", "null", "[]", "true", "\"text\"", "{}", "{} {}",
                "{username:'alice',password:'secret'}",
                "{\"username\":\"alice\",\"password\":\"secret\",}",
                "{\"username\":25,\"password\":\"secret\",\"email\":\"a@b.com\"}",
                "{\"username\":\"alice\",\"password\":true,\"email\":\"a@b.com\"}",
                "{\"username\":[],\"password\":\"secret\",\"email\":\"a@b.com\"}",
                "{\"username\":\"alice\",\"password\":{},\"email\":\"a@b.com\"}",
                "{\"username\":null,\"password\":\"secret\",\"email\":\"a@b.com\"}",
                "{\"username\":\"alice\",\"password\":null,\"email\":\"a@b.com\"}",
                "{\"username\":\"\",\"password\":\"secret\",\"email\":\"a@b.com\"}",
                "{\"username\":\"alice\",\"password\":\"   \",\"email\":\"a@b.com\"}");
    }

    @ParameterizedTest
    @MethodSource("invalidEmails")
    void registrationRequiresAnEmailString(String email) throws Exception {
        assertError(post("/user", "{\"username\":\"alice\",\"password\":\"secret\",\"email\":" + email + "}"),
                400, "Error: bad request");
        assertNull(data.getUser("alice"));
    }

    static Stream<String> invalidEmails() {
        return Stream.of("null", "25", "true", "[]", "{}", "\"\"", "\"  \"");
    }

    @Test
    void missingEmailIsBadRequest() throws Exception {
        assertError(post("/user", LOGIN), 400, "Error: bad request");
        assertNull(data.getUser("alice"));
    }

    @Test
    void quotedUnicodeCredentialsRoundTripWithoutTrimming() throws Exception {
        var request = new JsonObject();
        request.addProperty("username", "Al\"ice 雪");
        request.addProperty("password", " secret\\word ");
        request.addProperty("email", "alice@example.com");
        request.addProperty("ignoredExtraField", "not part of the result");
        var registration = post("/user", request.toString());
        assertEquals(200, registration.statusCode());
        assertEquals("Al\"ice 雪", json(registration).get("username").getAsString());
        assertEquals(200, post("/session", request.toString()).statusCode());
        request.addProperty("password", "secret\\word");
        assertError(post("/session", request.toString()), 401, "Error: unauthorized");
    }

    @Test
    void storageFailuresReturnGenericJsonOnBothEndpoints() throws Exception {
        server.stop();
        data = new MemoryDataAccess() {
            @Override
            public void createAuth(AuthData auth) throws DataAccessException {
                throw new DataAccessException("private database credentials");
            }
        };
        data.createUser(new UserData("alice", BCrypt.hashpw("secret", BCrypt.gensalt()), "alice@example.com"));
        server = new Server(data);
        port = server.run(0);
        assertError(post("/user", REGISTER.replace("alice", "bob")), 500, "Error: internal server error");
        assertError(post("/session", LOGIN), 500, "Error: internal server error");
    }

    @Test
    void unexpectedFailuresDoNotLeakInternalDetails() throws Exception {
        server.stop();
        server = new Server(new MemoryDataAccess() {
            @Override
            public UserData getUser(String username) {
                throw new IllegalStateException("private server diagnostics");
            }
        });
        port = server.run(0);
        assertError(post("/user", REGISTER), 500, "Error: internal server error");
        assertError(post("/session", LOGIN), 500, "Error: internal server error");
    }

    @Test
    void explicitMemoryServersDoNotShareUserStorage() throws Exception {
        server.stop();
        server = new Server(new MemoryDataAccess());
        port = server.run(0);
        assertEquals(200, post("/user", REGISTER).statusCode());
        server.stop();
        server = new Server(new MemoryDataAccess());
        port = server.run(0);
        assertError(post("/session", LOGIN), 401, "Error: unauthorized");
        assertEquals(200, post("/user", REGISTER).statusCode());
    }
}
