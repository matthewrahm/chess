package service;

import com.google.gson.Gson;
import dataaccess.DataAccessException;
import dataaccess.DuplicateUserException;
import dataaccess.MemoryDataAccess;
import model.AuthData;
import model.UserData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mindrot.jbcrypt.BCrypt;
import request.RegisterRequest;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static service.ServiceException.Code.*;

class RegisterServiceTests {
    private MemoryDataAccess data;
    private UserService service;
    private final RegisterRequest request = new RegisterRequest("matthew", "correct password", "matthew@example.com");

    @BeforeEach
    void setUp() {
        data = new MemoryDataAccess();
        service = new UserService(data);
    }

    @Test
    void registerStoresHashAndReturnsPersistedAuthorization() throws Exception {
        var result = service.register(request);
        UserData stored = data.getUser(request.username());
        assertEquals(request.username(), result.username());
        assertEquals(request.email(), stored.email());
        assertNotEquals(request.password(), stored.password());
        assertTrue(BCrypt.checkpw(request.password(), stored.password()));
        assertFalse(BCrypt.checkpw("wrong password", stored.password()));
        assertDoesNotThrow(() -> UUID.fromString(result.authToken()));
        assertEquals(new AuthData(result.authToken(), request.username()), data.getAuth(result.authToken()));
        var json = new Gson().toJsonTree(result).getAsJsonObject();
        assertEquals(Set.of("username", "authToken"), json.keySet());
    }

    @Test
    void differentUsersReceiveDifferentTokensAndSaltedHashes() throws Exception {
        var first = service.register(request);
        var second = service.register(new RegisterRequest("other", request.password(), "other@example.com"));
        assertNotEquals(first.authToken(), second.authToken());
        assertNotEquals(data.getUser(first.username()).password(), data.getUser(second.username()).password());
        assertTrue(BCrypt.checkpw(request.password(), data.getUser(second.username()).password()));
        assertNotNull(data.getAuth(first.authToken()));
        assertNotNull(data.getAuth(second.authToken()));
    }

    @ParameterizedTest
    @MethodSource("invalidRequests")
    void invalidRequestsCannotWriteUserOrAuthorization(RegisterRequest invalid) {
        var readOnly = new MemoryDataAccess() {
            @Override
            public void createUser(UserData user) {
                fail("Invalid registration must not create a user");
            }

            @Override
            public void createAuth(AuthData auth) {
                fail("Invalid registration must not create an authorization");
            }
        };
        var error = assertThrows(ServiceException.class, () -> new UserService(readOnly).register(invalid));
        assertEquals(BAD_REQUEST, error.getCode());
        assertEquals("Error: bad request", error.getMessage());
    }

    static Stream<RegisterRequest> invalidRequests() {
        return Stream.concat(Stream.of((RegisterRequest) null),
                Stream.of(null, "", "   ").flatMap(missing -> Stream.of(
                        new RegisterRequest(missing, "password", "email@example.com"),
                        new RegisterRequest("user", missing, "email@example.com"),
                        new RegisterRequest("user", "password", missing))));
    }

    @Test
    void duplicateRegistrationPreservesUserAndOriginalSession() throws Exception {
        var original = service.register(request);
        var stored = data.getUser(request.username());
        var duplicate = new RegisterRequest(request.username(), "replacement", "replacement@example.com");
        var error = assertThrows(ServiceException.class, () -> service.register(duplicate));
        assertEquals(ALREADY_TAKEN, error.getCode());
        assertEquals("Error: already taken", error.getMessage());
        assertEquals(stored, data.getUser(request.username()));
        assertEquals(new AuthData(original.authToken(), original.username()), data.getAuth(original.authToken()));
    }

    @Test
    void usernameConflictDuringInsertionIsStillAlreadyTaken() {
        var conflicting = new MemoryDataAccess() {
            @Override
            public void createUser(UserData user) throws DataAccessException {
                throw new DuplicateUserException();
            }

            @Override
            public void createAuth(AuthData auth) {
                fail("A duplicate registration must not create an authorization");
            }
        };
        var error = assertThrows(ServiceException.class, () -> new UserService(conflicting).register(request));
        assertEquals(ALREADY_TAKEN, error.getCode());
    }

    @Test
    void userStorageFailurePropagatesWithoutCreatingSession() {
        var failure = new DataAccessException("Simulated user storage failure");
        var failing = new MemoryDataAccess() {
            @Override
            public void createUser(UserData user) throws DataAccessException {
                throw failure;
            }

            @Override
            public void createAuth(AuthData auth) {
                fail("Failed user creation must not create an authorization");
            }
        };
        assertSame(failure, assertThrows(DataAccessException.class, () -> new UserService(failing).register(request)));
    }

    @Test
    void authorizationStorageFailureDoesNotReturnSuccess() {
        var failure = new DataAccessException("Simulated authorization storage failure");
        var failing = new MemoryDataAccess() {
            @Override
            public void createAuth(AuthData auth) throws DataAccessException {
                throw failure;
            }
        };
        assertSame(failure, assertThrows(DataAccessException.class, () -> new UserService(failing).register(request)));
    }
}
