package client;

import ui.ConsoleClient;
import ui.ConsoleInput;

public class ClientMain {
    public static void main(String[] args) {
        if (args.length > 1) {
            usage();
            return;
        }
        ServerFacade server;
        try {
            int port = args.length == 0 ? 8080 : Integer.parseInt(args[0]);
            server = new ServerFacade(port);
        } catch (IllegalArgumentException exception) {
            usage();
            return;
        }
        var input = new ConsoleInput(System.in, System.console());
        new ConsoleClient(server, input, System.out).run();
    }

    private static void usage() {
        System.out.println("Usage: java -jar client-test-dependencies.jar [port]");
        System.out.println("Choose a port from 1 to 65535. The default is 8080.");
    }
}
