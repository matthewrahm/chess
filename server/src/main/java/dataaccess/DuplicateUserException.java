package dataaccess;

/** A username conflict, including one detected after a service's initial lookup. */
public class DuplicateUserException extends DataAccessException {
    public DuplicateUserException() {
        super("Username already exists");
    }
}
