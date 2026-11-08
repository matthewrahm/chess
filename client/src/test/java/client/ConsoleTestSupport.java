package client;

import ui.ConsoleClient;
import ui.ConsoleInput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Shared console input/output fixture using the isolated HTTP and SQL server. */
abstract class ConsoleTestSupport extends ServerFacadeTestSupport {
    static String run(ServerFacade server, String commands) {
        var bytes = new ByteArrayOutputStream();
        var input = new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8));
        try (var output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new ConsoleClient(server, new ConsoleInput(input, null), output).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
