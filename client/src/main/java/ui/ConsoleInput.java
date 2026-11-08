package ui;

import java.io.Console;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

/** Reads terminal input, hiding passwords when a system console is available. */
public class ConsoleInput {
    private final Scanner scanner;
    private final Console console;

    public ConsoleInput(InputStream input, Console console) {
        scanner = new Scanner(input, StandardCharsets.UTF_8);
        this.console = console;
    }

    public String read(String prompt, PrintStream output, boolean secret) {
        output.print(prompt);
        output.flush();
        if (console == null) {
            return scanner.hasNextLine() ? scanner.nextLine() : null;
        }
        if (!secret) {
            return console.readLine();
        }
        char[] password = console.readPassword();
        if (password == null) {
            return null;
        }
        try {
            return new String(password);
        } finally {
            Arrays.fill(password, '\0');
        }
    }
}
