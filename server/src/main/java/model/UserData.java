package model;

/**
 * Represents a user in the chess application.
 *
 * @param username the unique username
 * @param password the hashed password
 * @param email the user's email address
 */
public record UserData(String username, String password, String email) {
}
