package client;

import java.util.NoSuchElementException;
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
            String line;
            try {
                line = scanner.nextLine();
            } catch (NoSuchElementException e) {
                break;
            }
            String result = client.eval(line);
            if (result.equals("quit")) {
                break;
            }
            if (!result.isEmpty()) {
                System.out.println(result);
            }
        }
    }
}
