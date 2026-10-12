package model;

/** A stored user; password contains a hash supplied by the service. */
public record UserData(String username, String password, String email) {
}
