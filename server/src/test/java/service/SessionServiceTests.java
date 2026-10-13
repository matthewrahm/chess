package service;

import chess.ChessGame;
import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import request.LoginRequest;
import request.LogoutRequest;
import request.RegisterRequest;
import result.RegisterResult;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static service.ServiceException.Code.*;

class SessionServiceTests {
    private MemoryDataAccess data;
    private UserService service;
    private RegisterResult registration;
    private final LoginRequest credentials = new LoginRequest("matthew", "correct password");

    @BeforeEach
    void setUp() throws Exception {
        data = new MemoryDataAccess();
        service = new UserService(data);
        registration = service.register(new RegisterRequest(credentials.username(), credentials.password(), "matthew@example.com"));
    }

    @Test
    void loginReturnsFreshPersistedSessionWithoutChangingUser() throws Exception {
        var storedUser = data.getUser(credentials.username());
        var result = service.login(credentials);
        assertEquals(credentials.username(), result.username());
        assertNotEquals(registration.authToken(), result.authToken());
        assertDoesNotThrow(() -> UUID.fromString(result.authToken()));
        assertEquals(new AuthData(result.authToken(), result.username()), data.getAuth(result.authToken()));
        assertNotNull(data.getAuth(registration.authToken()));
        assertEquals(storedUser, data.getUser(credentials.username()));
        var json = new Gson().toJsonTree(result).getAsJsonObject();
        assertEquals(Set.of("username", "authToken"), json.keySet());
    }

    @Test
    void eachLoginHasAnIndependentToken() throws Exception {
        var first = service.login(credentials);
        var second = service.login(credentials);
        assertEquals(3, Set.of(registration.authToken(), first.authToken(), second.authToken()).size());
        assertNotNull(data.getAuth(registration.authToken()));
        assertNotNull(data.getAuth(first.authToken()));
        assertNotNull(data.getAuth(second.authToken()));
    }

    @ParameterizedTest
    @MethodSource("invalidLoginRequests")
    void missingCredentialsAreBadRequests(LoginRequest invalid) {
        var error = assertThrows(ServiceException.class, () -> service.login(invalid));
        assertEquals(BAD_REQUEST, error.getCode());
        assertNotNull(data.getAuth(registration.authToken()));
    }

    static Stream<LoginRequest> invalidLoginRequests() {
        return Stream.concat(Stream.of((LoginRequest) null),
                Stream.of(null, "", "   ").flatMap(missing -> Stream.of(
                        new LoginRequest(missing, "password"), new LoginRequest("matthew", missing))));
    }

    @ParameterizedTest
    @MethodSource("incorrectCredentials")
    void invalidCredentialsCannotCreateAuthorization(LoginRequest incorrect) throws Exception {
        var noNewSessions = new MemoryDataAccess() {
            @Override
            public void createAuth(AuthData auth) {
                fail("A failed login must not create a session");
            }
        };
        noNewSessions.createUser(data.getUser(credentials.username()));
        var error = assertThrows(ServiceException.class, () -> new UserService(noNewSessions).login(incorrect));
        assertEquals(UNAUTHORIZED, error.getCode());
        assertEquals("Error: unauthorized", error.getMessage());
    }

    static Stream<LoginRequest> incorrectCredentials() {
        return Stream.of(new LoginRequest("unknown", "correct password"),
                new LoginRequest("matthew", "wrong password"),
                new LoginRequest("Matthew", "correct password"),
                new LoginRequest("matthew", "Correct password"));
    }

    @Test
    void passwordsAreNotTrimmedDuringRegistrationOrLogin() throws Exception {
        service.register(new RegisterRequest("spaces", " password ", "spaces@example.com"));
        assertNotNull(service.login(new LoginRequest("spaces", " password ")).authToken());
        var error = assertThrows(ServiceException.class, () -> service.login(new LoginRequest("spaces", "password")));
        assertEquals(UNAUTHORIZED, error.getCode());
    }

    @Test
    void loginPropagatesAuthorizationStorageFailure() throws Exception {
        var failure = new DataAccessException("Simulated authorization write failure");
        var failing = new MemoryDataAccess() {
            @Override
            public void createAuth(AuthData auth) throws DataAccessException {
                throw failure;
            }
        };
        failing.createUser(data.getUser(credentials.username()));
        assertSame(failure, assertThrows(DataAccessException.class, () -> new UserService(failing).login(credentials)));
    }

    @Test
    void logoutRemovesOnlyRequestedSessionAndPreservesUserAndGames() throws Exception {
        var anotherSession = service.login(credentials);
        var otherUser = service.register(new RegisterRequest("other", "other password", "other@example.com"));
        int gameID = data.createGame("Practice", new ChessGame());
        var storedGame = data.getGame(gameID);
        var storedUser = data.getUser(credentials.username());

        service.logout(new LogoutRequest(registration.authToken()));

        assertNull(data.getAuth(registration.authToken()));
        assertNotNull(data.getAuth(anotherSession.authToken()));
        assertNotNull(data.getAuth(otherUser.authToken()));
        assertEquals(storedGame, data.getGame(gameID));
        assertEquals(storedUser, data.getUser(credentials.username()));
    }

    @ParameterizedTest
    @MethodSource("invalidLogoutRequests")
    void missingAndUnknownTokensAreUnauthorized(LogoutRequest invalid) {
        var error = assertThrows(ServiceException.class, () -> service.logout(invalid));
        assertEquals(UNAUTHORIZED, error.getCode());
        assertEquals("Error: unauthorized", error.getMessage());
        assertNotNull(data.getAuth(registration.authToken()));
    }

    static Stream<LogoutRequest> invalidLogoutRequests() {
        return Stream.of(null, new LogoutRequest(null), new LogoutRequest(""),
                new LogoutRequest("   "), new LogoutRequest("unknown-token"));
    }

    @Test
    void revokedTokenCannotLogOutAgainButUserCanLogInAgain() throws Exception {
        var logout = new LogoutRequest(registration.authToken());
        service.logout(logout);
        var error = assertThrows(ServiceException.class, () -> service.logout(logout));
        assertEquals(UNAUTHORIZED, error.getCode());
        var newSession = service.login(credentials);
        assertNotEquals(registration.authToken(), newSession.authToken());
        assertNotNull(data.getAuth(newSession.authToken()));
        assertNull(data.getAuth(registration.authToken()));
    }
}
