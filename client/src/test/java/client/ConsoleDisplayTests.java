package client;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleDisplayTests extends ConsoleTestSupport {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void authenticationEscapesDisplayControlsWithoutChangingAccountIdentity(boolean register) throws Exception {
        String username = "Zoë\u001b[2J\t\u202e王";
        if (!register) {
            facade.register(username, "secret", "p@example.com");
        }
        String commands = (register ? "register\n" : "login\n") + username + "\nsecret\n"
                + (register ? "p@example.com\n" : "") + "create\nPractice\nplay\n1\nwhite\nback\nlist\nlogout\nquit\n";
        String output = run(facade, commands);
        assertTrue(output.contains("Signed in as Zoë [2J  王."));
        assertTrue(output.contains("White: Zoë [2J  王"));
        assertFalse(output.contains(username));
        assertFalse(output.contains("\u001b[2J"));
        assertFalse(output.contains("\u202e"));
        assertEquals(username, storage.getUser(username).username());
        assertEquals(username, storage.listGames().iterator().next().whiteUsername());
        assertTrue(output.contains("Signed out."));
    }
}
