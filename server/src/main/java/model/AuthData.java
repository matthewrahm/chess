package model;

/**
 * Represents an authentication session linking a token to a user.
 *
 * @param authToken the unique authentication token
 * @param username the username associated with this token
 */
public record AuthData(String authToken, String username) {
}
