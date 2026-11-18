package service;

/** A rejected gameplay command whose explanation may be returned to its sender. */
public class GameplayException extends Exception {
    public GameplayException(String message) {
        super(message);
    }
}
