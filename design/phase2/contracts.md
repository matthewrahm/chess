# Phase 2 design contracts

These are proposed interfaces for Phase 3, not implemented server code.
The [sequence diagram](server-design.uml) follows the course's Server -> Handler
-> Service -> DataAccess -> Database layering. The Database lifeline represents
storage: Phase 3 uses memory; Phase 4 replaces it with SQL behind the data access
interface. Handler owns HTTP/JSON; Service owns validation and business rules;
DataAccess owns storage operations.

The [course API specification](https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/3-web-api/web-api.md#endpoint-specifications)
defines the HTTP methods, URLs, field names, and required responses.

## Service requests and results

All textual fields are String. gameID is int in stored data and results, and
Integer in JoinGameRequest to distinguish a missing ID. Protected request
objects take authToken from the authorization header, never a JSON username.

| Method | Request fields | Result | HTTP statuses |
| --- | --- | --- | --- |
| register | username, password, email | RegisterResult(username, authToken) | 200, 400, 403, 500 |
| login | username, password | LoginResult(username, authToken) | 200, 400, 401, 500 |
| logout | authToken | void; handler returns {} | 200, 401, 500 |
| clear | none | void; handler returns {} | 200, 500 |
| listGames | authToken | ListGamesResult(Collection<GameSummary> games) | 200, 401, 500 |
| createGame | authToken, gameName | CreateGameResult(int gameID) | 200, 400, 401, 500 |
| joinGame | authToken, playerColor, Integer gameID | void; handler returns {} | 200, 400, 401, 403, 500 |

GameSummary contains gameID, whiteUsername, blackUsername, and gameName.
Player names may be null. The list response includes all games, excludes the
ChessGame state, and uses an empty array when no games exist.

## Models and data access

Use the course's [model record definitions](https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/3-web-api/web-api.md#data-model-classes)
for UserData, AuthData, and GameData. UserData.password stores a password hash;
request passwords are verified in the service and never returned. Registration
and every successful login generate a fresh UUID token and persist AuthData.

| Proposed data access method | Result |
| --- | --- |
| getUser(String username) | UserData or null |
| createUser(UserData user) | void |
| getAuth(String authToken) | AuthData or null |
| createAuth(AuthData auth) | void |
| deleteAuth(String authToken) | void; removes that token only |
| clear() | void; removes authorizations, games, and users |
| listGames() | Collection<GameData>; never null |
| createGame(String gameName, ChessGame game) | int unique gameID; both seats initially null |
| getGame(int gameID) | GameData or null |
| updateGame(GameData game) | void; replaces the existing game by ID |

Each data access method can throw DataAccessException. Services propagate that
exception and handlers translate it into 500 with a JSON error message. A failed
operation stops the success flow; the storage-error groups are alternatives,
not additional responses after success.

## Validation and preservation

- Malformed JSON, incompatible field types, and missing required request fields
  become 400. Missing/invalid authorization becomes 401. Clear requires no token.
- Unknown usernames and incorrect passwords both produce the same 401 response.
  Duplicate registration and occupied requested seats produce 403.
- Join rejects missing games and colors other than WHITE or BLACK with 400. It
  resolves the username from AuthData and checks availability before updating.
- Join changes only the selected player field; it preserves the other player,
  game name, board, and turn. The same account may occupy both different seats.
- Creation initializes the standard ChessGame with White to move; it does not
  join the creator. Logout preserves users, games, and other login sessions.
- Error bodies use a message beginning with "Error:". Responses never expose
  passwords, password hashes, or internal database diagnostics.
