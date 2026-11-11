package client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleSessionTests extends ConsoleTestSupport {
    private static final String LOGIN = "login\nplayer\nsecret\n";
    private static final String GAMES = "{\"games\":[{\"gameID\":600,\"gameName\":\"Practice\"}]}";

    @ParameterizedTest
    @ValueSource(strings = {"list", "create", "join", "logout"})
    void expiredSessionReturnsToLoginAndAllowsFreshSession(String operation) throws Exception {
        var logins = new AtomicInteger();
        var rejected = new AtomicBoolean();
        try (var http = new TestHttpServer(request -> {
            if (request.method().equals("POST") && request.path().equals("/session")) {
                return authentication(logins.incrementAndGet());
            }
            boolean target = switch (operation) {
                case "list" -> request.method().equals("GET");
                case "create" -> request.method().equals("POST");
                case "join" -> request.method().equals("PUT");
                default -> request.method().equals("DELETE");
            };
            if (target && !rejected.getAndSet(true)) {
                return new TestHttpServer.Response(401, "private details");
            }
            return new TestHttpServer.Response(200, GAMES);
        })) {
            String command = switch (operation) {
                case "list" -> "list\n";
                case "create" -> "create\nPractice\n";
                case "join" -> "play\n1\nwhite\n";
                default -> "observe\n1\nlogout\n";
            };
            String output = run(new ServerFacade(http.port()), LOGIN + command + LOGIN + "list\nquit\n");
            assertTrue(rejected.get());
            assertEquals(2, logins.get());
            assertTrue(output.contains("Your session is no longer valid. Please log in again."));
            String after = output.substring(output.indexOf("Your session is no longer valid."));
            assertTrue(after.contains("[SIGNED OUT] >>>"));
            assertTrue(after.contains("1. Practice"));
            assertFalse(after.contains("[BOARD]"));
            assertFalse(output.contains("private details"));
            assertFalse(output.contains("private-token"));
            assertEquals("private-token-2", http.request().authorization());
        }
    }

    @Test
    void temporaryJoinFailureKeepsLoginAndCanBeRetried() throws Exception {
        var joins = new AtomicInteger();
        var logins = new AtomicInteger();
        try (var http = new TestHttpServer(request -> {
            if (request.path().equals("/session")) {
                return authentication(logins.incrementAndGet());
            }
            if (request.method().equals("PUT")) {
                return new TestHttpServer.Response(joins.incrementAndGet() == 1 ? 500 : 200, "{}");
            }
            return new TestHttpServer.Response(200, GAMES);
        })) {
            String output = run(new ServerFacade(http.port()), LOGIN + "play\n1\nwhite\nplay\n1\nwhite\nquit\n");
            assertEquals(1, logins.get());
            assertEquals(2, joins.get());
            assertTrue(output.contains("The server could not complete the request."));
            assertEquals(1, output.split("Joined as white", -1).length - 1);
            assertEquals("private-token-1", http.request().authorization());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"login", "register"})
    void unusableAuthenticationTokenLeavesConsoleSignedOutAndAllowsRetry(String operation) throws Exception {
        var logins = new AtomicInteger();
        var protectedRequests = new AtomicInteger();
        try (var http = new TestHttpServer(request -> {
            if (request.method().equals("POST")) {
                if (logins.incrementAndGet() == 1) {
                    return new TestHttpServer.Response(200,
                            "{\"username\":\"player\",\"authToken\":\"private-token\\nvalue\"}");
                }
                return authentication(logins.get());
            }
            protectedRequests.incrementAndGet();
            return new TestHttpServer.Response(200, GAMES);
        })) {
            String first = operation + "\nplayer\nsecret\n" + (operation.equals("register") ? "p@example.com\n" : "");
            String output = run(new ServerFacade(http.port()), first + "list\n" + LOGIN + "list\nquit\n");
            assertEquals(2, logins.get());
            assertEquals(1, protectedRequests.get());
            assertTrue(output.contains("The server returned an invalid"));
            assertEquals(1, output.split("Signed in as player.", -1).length - 1);
            String beforeRetry = output.substring(0, output.indexOf("Signed in as player."));
            assertFalse(beforeRetry.contains("[SIGNED IN]"));
            assertTrue(beforeRetry.contains("Please log in first."));
            assertTrue(output.contains("1. Practice"));
            assertFalse(output.contains("private-token"));
            assertEquals("private-token-2", http.request().authorization());
        }
    }

    private TestHttpServer.Response authentication(int number) {
        return new TestHttpServer.Response(200,
                "{\"username\":\"player\",\"authToken\":\"private-token-" + number + "\"}");
    }
}
