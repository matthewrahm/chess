package client;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mindrot.jbcrypt.BCrypt;
import ui.ConsoleClient;
import ui.ConsoleInput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/** Exercises real command text through the real HTTP facade and isolated SQL server. */
class ConsoleClientTests extends ServerFacadeTestSupport {
    @Test
    void helpUnknownCommandsAndQuitWorkWithoutConnecting() throws Exception {
        var offline = new ServerFacade(TestHttpServer.stoppedPort());
        String output = run(offline, "\n  HELP  \nunknown\n QuIt \nregister\n");
        assertTrue(output.contains("login    Sign in"));
        assertTrue(output.contains("register Create"));
        assertTrue(output.contains("Unknown command."));
        assertTrue(output.endsWith("Goodbye!\n"));
        assertFalse(output.contains("Username:"));
        assertFalse(output.contains("Unable to reach"));
    }

    @Test
    void registrationSignsInAutomaticallyAndLogoutReturnsToPrelogin() throws Exception {
        String output = run(facade, "register\nZoë 王\n secret \np@example.com\nhelp\nlogout\nquit\n");
        assertTrue(output.contains("Signed in as Zoë 王."));
        assertTrue(output.contains("[SIGNED IN] >>>"));
        assertTrue(output.contains("Signed out.\n"));
        assertTrue(output.substring(output.indexOf("Signed out.\n")).contains("[SIGNED OUT] >>>"));
        assertTrue(BCrypt.checkpw(" secret ", storage.getUser("Zoë 王").password()));
        assertFalse(output.contains(" secret "));
    }

    @Test
    void incorrectLoginCanRetryAndSignOutWithoutRevokingOtherSessions() throws Exception {
        var existing = facade.register("player", "secret", "p@example.com");
        String output = run(facade, "login\nplayer\nwrong\nlogin\nplayer\nsecret\nlogout\nquit\n");
        assertTrue(output.contains("Incorrect username or password."));
        assertTrue(output.contains("Signed in as player."));
        assertTrue(output.contains("Signed out.\n"));
        assertNotNull(storage.getAuth(existing.authToken()));
        assertFalse(output.contains(existing.authToken()));
    }

    @Test
    void duplicateRegistrationCanRetryWithoutReplacingOriginalAccount() throws Exception {
        facade.register("player", "original", "first@example.com");
        var original = storage.getUser("player");
        String output = run(facade, "register\nplayer\nnew\nnew@example.com\n"
                + "register\nother\nsecret\no@example.com\nquit\n");
        assertTrue(output.contains("That username is already taken."));
        assertTrue(output.contains("Signed in as other."));
        assertEquals(original, storage.getUser("player"));
        assertNotNull(storage.getUser("other"));
    }

    @Test
    void signedInUsersMustLogOutBeforeAuthenticatingAgain() throws Exception {
        String output = run(facade, "register\nplayer\nsecret\np@example.com\nlogin\nregister\nquit\n");
        assertTrue(output.contains("You are already signed in."));
        assertEquals(1, output.split("Username:", -1).length - 1);
        assertNotNull(storage.getUser("player"));
    }

    @Test
    void logoutWhenSignedOutDoesNotContactTheServer() throws Exception {
        String output = run(new ServerFacade(TestHttpServer.stoppedPort()), "logout\nquit\n");
        assertTrue(output.contains("You are already signed out."));
        assertFalse(output.contains("Unable to reach"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "login\n", "login\nplayer\n", "register\nplayer\nsecret\n"})
    void endOfInputExitsWithoutSendingIncompleteCredentials(String commands) throws Exception {
        try (var http = new TestHttpServer(500, "private details")) {
            String output = run(new ServerFacade(http.port()), commands);
            assertTrue(output.endsWith("Goodbye!\n"));
            assertNull(http.request());
        }
    }

    @Test
    void serverOutageKeepsPreloginUsableWithoutPrintingTechnicalDetails() throws Exception {
        String output = run(new ServerFacade(TestHttpServer.stoppedPort()), "login\nplayer\nsecret\nhelp\nquit\n");
        assertTrue(output.contains("Unable to reach the server. Please try again."));
        assertTrue(output.substring(output.indexOf("Unable to reach")).contains("[SIGNED OUT] >>>"));
        assertFalse(output.contains("Exception"));
        assertFalse(output.contains("secret"));
    }

    @Test
    void sessionTokensAreNeverPrintedAndLogoutSendsTheReceivedToken() throws Exception {
        try (var http = new TestHttpServer(200, "{\"username\":\"player\",\"authToken\":\"private-token\"}")) {
            String output = run(new ServerFacade(http.port()), "login\nplayer\nsecret\nlogout\nquit\n");
            assertTrue(output.contains("Signed in as player."));
            assertTrue(output.contains("Signed out.\n"));
            assertEquals("private-token", http.request().authorization());
            assertFalse(output.contains("private-token"));
            assertFalse(output.contains("authToken"));
        }
    }

    @Test
    void malformedLoginResponseDoesNotEnterSignedInState() throws Exception {
        try (var http = new TestHttpServer(200, "{}")) {
            String output = run(new ServerFacade(http.port()), "login\nplayer\nsecret\nquit\n");
            assertTrue(output.contains("invalid login response"));
            assertFalse(output.contains("[SIGNED IN]"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad", "0", "-1", "65536"})
    void mainRejectsInvalidPortWithoutAStackTrace(String port) {
        var bytes = new ByteArrayOutputStream();
        var original = System.out;
        try (var output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            ClientMain.main(new String[]{port});
        } finally {
            System.setOut(original);
        }
        assertTrue(bytes.toString(StandardCharsets.UTF_8).contains("Usage:"));
        assertFalse(bytes.toString(StandardCharsets.UTF_8).contains("Exception"));
    }

    private String run(ServerFacade server, String commands) {
        var bytes = new ByteArrayOutputStream();
        var input = new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8));
        try (var output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new ConsoleClient(server, new ConsoleInput(input, null), output).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
