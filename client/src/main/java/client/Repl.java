package client;

import java.util.Scanner;

public class Repl {

    private final ChessClient client;

    public Repl(int port) {
        this.client = new ChessClient(port);
    }

    public void run() {
        System.out.println("♕ 240 Chess Client. Type 'help' to get started.");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            String prompt = (client.getState() == ChessClient.State.LOGGED_OUT)
                    ? "[LOGGED_OUT] >>> "
                    : "[" + client.getUsername() + "] >>> ";
            System.out.print(prompt);
            String line = scanner.nextLine();
            String result = client.eval(line);
            if (result.equals("quit")) {
                break;
            }
            System.out.println(result);
        }
    }
}
