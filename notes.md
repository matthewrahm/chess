# My Project Notes

Matthew Rahm

Made first repository and two commits.

## Repository setup

This BYU CS 240 chess project was created from the course template.
The repository is https://github.com/matthewrahm/chess.

- `client` contains the command-line chess client.
- `server` manages users, games, and network requests.
- `shared` contains chess rules and game state used by the client and server.
- `starter-code` contains the course files for each development phase.

## Git workflow

1. Pull the latest changes before starting work.
2. Make a small, cohesive change and verify it.
3. Review the changes with `git diff` and `git status`.
4. Stage the relevant files, commit with a descriptive message, and push.

Update these notes with techniques and technologies learned throughout the course.

## Phase 0 review plan

The repository already contains the previous implementation. These eight steps
review and separate the movement rules without resetting the project.

1. Move king rules into a calculator with a shared move-validation helper.
2. Move knight rules into a calculator and reuse the helper.
3. Extract rook rules and shared sliding movement.
4. Extract bishop rules using the sliding helper.
5. Extract queen rules using the sliding helper.
6. Extract pawn rules, including double moves, captures, and promotion.
7. Review board setup and simplify repeated placement code.
8. Review movement edge cases and run the full Phase 0 checks.

The public chess method signatures stay the same. Movement calculators handle
piece rules; check and turn rules remain in ChessGame.

### Current progress

Steps 1 through 8 are complete on `main`. All six pieces use separate movement
calculators. Board reset clears each row with Arrays.fill and uses one placement
helper for both teams. Public chess method signatures are unchanged.
Added edge-case tests for resetting a changed board, finding moves without
changing the board, pawns at the last rank, and moves that expose the king.
The last case belongs in pieceMoves because ChessGame handles check rules.
`mvn -pl shared test`: 123 tests passed, with no failures or errors.
`mvn -DskipTests package`: all three modules built successfully.
The checks above describe the September 18 snapshot before the later-phase reset.

## Fall 2026 starting point

The September Phase 0 implementation and tests are preserved. ChessGame and the
client/server entry points are restored to the original starter versions. Older
Phase 1 and later implementations and their active tests were removed; the
supplied files remain in starter-code for use when each phase begins.
All previous commits remain in Git history.

Validation after reset: `mvn clean verify` with Java 21 passed all 68 Phase 0
tests and built all three modules. Phase 0 source and tests are unchanged from
the September 18 commit; restored stubs match the original starter.

## Phase 1 progress

Day 1, commit 1: initialize a standard board and White's turn, expose board and
turn accessors, and compare games by board and turn. Setup tests also check that
separate games do not share mutable state.

Validation: `mvn -pl shared test` with Java 21 passed 72 tests.
Day 1, commit 2: locate each team's king and detect attacks using the opponent's
pieceMoves. Detection ignores the current turn and does not modify the board.
Tests cover every piece and both colors, blocked attacks, pawn direction, and a
pinned attacking piece.

Validation: `mvn clean verify` with Java 21 passed all 79 active tests and built
all three modules. The full Phase 1 passoff suite is not active yet because move
execution, legal-move filtering, checkmate, and stalemate remain unimplemented.

Day 2, commit 3: simulate a candidate move on a copied board, including captures
and promotion. Check detection can inspect a supplied board without swapping out
the current board. Simulation tests verify that the original pieces and turn
stay unchanged.

Validation: `mvn -pl shared test` with Java 21 passed all 82 active tests.

Day 2, commit 4: validMoves filters the existing piece moves using a simulated
board and the moving team's king safety. Empty squares return null, and move
queries work independently of whose turn it is. Activated the six supplied
ValidMovesTests unchanged and added checks for game preservation, protected
captures, and promotion captures that remove check.

Validation: `mvn clean verify` with Java 21 passed all 92 active tests and built
all three modules. A new promotion test initially allowed an unintended blocking
move; corrected its board setup to isolate capturing the checking rook. The
implementation passed the supplied valid-move tests on the first run.

Day 3, commit 5: makeMove now rejects missing or out-of-bounds positions, empty
starting squares, wrong-turn moves, and moves outside validMoves. Promotion
choices are checked as part of matching a legal move. Rejected requests leave
the board and turn unchanged. Accepted-move execution is the next step.

Validation: `mvn -pl shared test` with Java 21 passed all 98 active tests.

Day 3, commit 6: accepted moves now update the existing board, replace captured
pieces, promote pawns, and switch turns. A shared applyMove helper keeps move
simulation and execution consistent. Activated the supplied MakeMoveTests and
ChessGameTests unchanged. Additional tests cover a capture sequence, rejection
between legal moves, escaping check by capture, and every promotion choice for
both teams with and without capture.

Validation: `mvn clean verify` with Java 21 passed all 127 active tests and built
all three modules. All 22 supplied MakeMoveTests and three ChessGameTests pass.

Day 4, commit 7: checkmate requires check and no legal move from any piece on
the requested team. A shared hasLegalMove helper checks the team's pieces and
stops when it finds an escape. Tests cover both teams, independence from turn,
unchanged game state, and escapes by another piece's capture or block.

Validation: `mvn -pl shared test` with Java 21 passed all 131 active tests.

Day 4, commit 8: stalemate requires no check and no legal moves from any piece
on the requested team, reusing hasLegalMove. Activated the supplied GameStatusTests
and FullGameTest unchanged. Additional tests distinguish mate from stalemate,
check both colors and turns, preserve board state, and confirm that another
piece's legal move prevents stalemate even when the king cannot move.

Validation: `mvn clean verify` with Java 21 passed all 147 active tests and built
all three modules. All five supplied core Phase 1 test files are now active and
match their starter copies exactly (43 core Phase 1 test cases total).

Day 5, commit 9: castling checks the king and rook's remaining rights, clear
squares, and king safety before, during, and after the move. Execution moves both
pieces. Moving a king or moving/capturing a corner rook removes the appropriate
rights; queries and rejected moves preserve them. Game equality includes these
rights, and setBoard starts a fresh position with reset rights.

Validation: `mvn -pl shared test` with Java 21 passed all 159 active tests,
including all seven supplied CastlingTests and five additional edge-case tests.

Day 5, commit 10: en passant is available only after an adjacent opposing pawn's
double move and expires on the next accepted move. Simulation and execution
remove the captured pawn from its actual square, so king safety accounts for
both vacated squares. Queries and rejected moves preserve eligibility; setBoard
clears it. Equality includes the en passant state.

Validation: `mvn clean verify` with Java 21 passed all 169 active tests and built
all three modules. All seven supplied Phase 1 test files, including castling and
en passant, are active and unchanged from the starter copies. Additional tests
cover discovered checks, escaping pawn check, eligibility expiration, and state
preservation. No unimplemented shared chess methods remain.

Final review, commit 11: reject moves that capture the opposing king. Custom
board positions previously allowed this because simulating the capture removed
the king before the safety check. validMoves now excludes king captures while
isInCheck still uses pieceMoves to detect attacks. Two regression tests cover
all six piece types for both colors and verify rejected moves preserve state.
Both tests failed before the fix and pass afterward.

Validation: `mvn clean verify` with Java 21 passed all 171 active tests and built
all three modules, including all supplied core and extra-credit Phase 1 tests.

Progress: all 10 planned commits plus one final correctness fix (11 Phase 1
commits total). Later phases remain at their starter state. No grading submission
made yet; submit the current GitHub revision through the course autograder and
review its result.

## Phase 2 plan and progress

Phase 2 is server design, with no starter code or Java implementation required.
The deliverable is a SequenceDiagram.org presentation link covering seven
endpoints. The README links to the current diagram; its editable source is
`design/phase2/server-design.uml`. Phase 3 implements these proposed interactions.

Instructions: https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/2-server-design/server-design.md
API contract: https://github.com/softwareconstruction240/softwareconstruction/blob/main/chess/3-web-api/web-api.md
The course manifest maps the supplied MasteryLS topic ID to this Phase 2 page.

Nine planned commits, two per workday and one on the final day:
1. Register: layered request flow, username availability, user/token creation.
2. Login: credential verification and creation of a fresh session.
3. Logout: token lookup and removal of the requested session only.
4. Clear: remove users, games, and authorization data.
5. List games: authorize the caller and return the public game list.
6. Create game: authorize, initialize, persist, and return the game ID.
7. Join game: authorize, validate the requested seat, and update the game.
8. Review error propagation and align the shared method/model contracts.
9. Review all seven flows together and finalize the presentation link for submission.

Day 1, commit 1: designed Register using the course's six lifelines. The handler
owns JSON and HTTP; the service validates input, checks username availability,
and creates a user and fresh authorization; data access owns storage calls.
The diagram includes 400/403 outcomes and an alternative 500 propagation flow.
`UserData.password` stores the password hash; `AuthData` carries authToken and
username. RegisterRequest has username/password/email strings; RegisterResult
has username/authToken strings. These interfaces are proposed, not implemented.

Validation: compared the Register flow to the published API and starter example;
checked the presentation link decompresses exactly to the saved UML source.
Browser automation is unavailable, so in-browser rendering remains to be checked.
No Java source or earlier-phase behavior changed. Working branch: `main`.

Day 1, commit 2: designed Login with request validation, a user lookup, safe
handling of a missing user before password verification, and a fresh stored
AuthData for every successful login. Unknown users and wrong passwords both
produce 401; missing fields or malformed JSON produce 400. Storage errors
propagate to the handler and become 500 responses. Existing sessions remain.
LoginRequest and LoginResult each carry two strings: username/password and
username/authToken respectively. Password verification belongs in the service;
passwords and hashes are absent from the response.

Validation: reviewed both endpoints against the Phase 3 request/response and
error contracts. UML group boundaries and participant references checked;
updated presentation link round-trips to the exact combined source. No runtime
code changed, so Maven tests were not rerun for this diagram-only work.
Progress: 2 of 9 planned Phase 2 commits. Next: Logout and Clear designs.
The diagram is incomplete and has not been submitted to Canvas.

Day 2, commit 3 (October 5): designed Logout. The handler reads the authorization
header into LogoutRequest(authToken), and the service rejects missing or unknown
tokens before deleting only that token. Users, games, and other sessions remain.
Success returns 200 with {}; authorization failures return 401, and storage
failures propagate through the service to a 500 response. Proposed interfaces:
void logout(LogoutRequest), AuthData getAuth(String authToken), and
void deleteAuth(String authToken); data access may throw DataAccessException.

Validation: rechecked the current course Logout contract, diagram group balance,
participant references, lookup-before-delete ordering, and exact presentation-link
round-trip. No Java code changed; Maven was not rerun. Browser rendering remains
unverified. Branch: main. Progress: 3 of 9 Phase 2 commits, with one commit today
as requested. Next: Clear. The full diagram is still in progress, not submitted.

Day 3, commit 4 (October 6): designed Clear application, DELETE /db. The handler
calls the service's clear(), which delegates to DataAccess.clear(). Storage
removes all authorizations, games, and users before returning success. No token
or body is required, and clearing an empty store succeeds. The handler returns
200 with {}; DataAccessException propagates through the service and becomes 500.
Proposed interfaces: void Service.clear() and void DataAccess.clear(), with the
latter allowed to throw DataAccessException. No Java implementation added.

Validation: checked all three data categories, the no-auth contract, empty-store
behavior, and 200/500 responses against the current course API. Presentation-link
round-trip and whitespace checks passed. Progress: 4 of 9 Phase 2 commits.

Day 3, commit 5 (October 6): designed List Games, GET /game. The handler reads
an authorization header; the service validates it before reading any games.
The service projects GameData into GameSummary(gameID, whiteUsername,
blackUsername, gameName), excluding ChessGame state. The response is 200 with a
games array, including [] for an empty store and null for unoccupied seats.
All games are listed, regardless of the caller, without modifying stored data.
Invalid authorization produces 401; storage failures propagate to a 500 response.

Proposed interfaces: ListGamesResult listGames(ListGamesRequest),
AuthData getAuth(String authToken), and Collection<GameData> listGames().
ListGamesRequest carries a String authToken; ListGamesResult carries a collection
of GameSummary values with an int gameID and three String fields (the two player
names are nullable). The shared API specification remains the contract reference.

Validation: reviewed authorization-before-read ordering, exact response field
names, empty lists, nullable players, no state mutation, and 401/500 propagation.
Checked all diagram group boundaries and participant references and verified the
README presentation link decompresses to the complete saved source. No Java
changes; Maven was not rerun. Browser rendering remains unverified.
Progress: 5 of 9 Phase 2 commits on main. Next: Create Game and Join Game.
The full diagram is still in progress and has not been submitted to Canvas.

Day 4, commit 6 (October 7): designed Create Game, POST /game. The handler combines
an authorization header and JSON gameName into CreateGameRequest. The service
validates the token and required gameName, initializes ChessGame, and asks data
access to store a new GameData with a unique ID and two empty player seats.
Success returns 200 with gameID only. Creating a game does not automatically
join the caller. Invalid input, authorization, and storage map to 400/401/500.
Proposed interfaces: CreateGameResult createGame(CreateGameRequest) and
int DataAccess.createGame(String gameName, ChessGame game). The request contains
String authToken/gameName; the result contains int gameID.

Validation: checked the current Create Game API, authorization-before-creation,
initial board/turn, null player names, unique ID, and response/error contracts.
Presentation-link round-trip and whitespace checks passed. No Java code changed.
Progress: 6 of 9 Phase 2 commits on main. Next: Join Game.

Day 4, commit 7 (October 7): designed Join Game, PUT /game. The service resolves
the caller from AuthData, validates the requested color and game ID, loads the
game, and rejects an occupied seat before updating it. WHITE and BLACK have
separate branches. The update preserves the other player, game name, ChessGame
board, and turn. The client cannot supply a replacement username. Successful
requests return 200 with {}; errors cover 400/401/403/500. Joining both different
seats with the same account remains possible, as the supplied API tests require.

Proposed interfaces: void joinGame(JoinGameRequest), GameData getGame(int gameID),
and void updateGame(GameData game). JoinGameRequest contains String authToken,
String playerColor, and Integer gameID so missing IDs can be distinguished from
present values. Data access may throw DataAccessException. These are design
contracts for Phase 3, not implemented Java APIs.

Validation: compared Create/Join to the current course API and supplied Phase 3
passoff scenarios (read only). Checked authorization-before-game-access,
existence/seat checks before mutation, both color branches, field preservation,
all seven endpoint flows' group balance/participant references, and the exact
README presentation-link round-trip. No Java changes; Maven was not rerun.
Browser rendering remains unverified. Progress: 7 of 9 Phase 2 commits on main.
Next: review shared contracts/error propagation, then finalize and visually
verify the complete presentation. No Canvas submission has been made.

Final review, commit 8 (October 8): aligned the sequence diagram's proposed
interfaces in `design/phase2/contracts.md`. Named lookup results (user/auth/game)
and generated passwordHash/authToken values explicitly, and clarified null
requests and malformed/wrong-type JSON handling. The contract records the exact
seven endpoint status sets, model fields, nullable seats, unique IDs, session
preservation, and separation between HTTP handling, service logic, and storage.
It also distinguishes Phase 3 memory storage from the later SQL implementation.

Validation: refreshed and reviewed the official Phase 2 and Phase 3 specs;
SequenceDiagram.org's own JavaScript renderer parsed the diagram with zero
syntax errors and produced a PNG through its backend export API. README link
round-trip passed. Visual layout review is next. Progress: 8 of 9 commits.

Final presentation, commit 9 (October 8): replaced dark group fills with light
colors so black labels and arrows remain readable, wrapped long request/result
labels, and moved Create/Join null-request checks before request-field access.
README now links the completed presentation, editable source, and proposed
contracts, with the Canvas submission step. All seven endpoint designs are done.

Validation: rendered the complete source using SequenceDiagram.org's own backend
renderer with zero syntax errors; visually inspected all seven endpoint sections
and their error alternatives. Fixed the canvas adapter to honor export dimensions
before visual inspection. Regenerated the presentation link and verified it
decodes to the exact final source. Java 21 `mvn -B clean verify` succeeded across
all modules: 171 tests, zero failures/errors/skips. `git diff --check` passed.
No Java implementation changed; these tests verify the existing chess code, not
a Phase 3 server implementation. Browser UI access remains unavailable; diagram
rendering was verified through the tool's backend API instead.

Handoff: main; Phase 2 design complete at 9 of 9 planned commits. Next step:
submit the README presentation URL to Canvas and review any TA feedback.
No Canvas submission has been made. Phase 3 implementation has not started.

## Phase 3: Chess Web API

Deadline confirmed: Thursday, October 22, 2026. The current course rubric requires
at least 12 commits spread over the assignment period (rather than the initially
requested 11). Start with one commit per day; one day will need a second meaningful
commit. Daily work resumes when requested, not through an automatic schedule.
Baseline: main at d46179d. Preserve the Phase 2 design in
`design/phase2/contracts.md` and `design/phase2/server-design.uml`.

Planned implementation milestones (adjust based on actual findings):
1. Shared models and in-memory data access, with storage tests.
2. Registration service, password hashing, and validation tests.
3. Login/logout services and independent session tests.
4. List/create game services and response models.
5. Join game service, seat validation, and preservation tests.
6. Clear service and cross-service lifecycle tests.
7. Course Javalin bootstrap, static test client, and server lifecycle checks.
8. Registration/login HTTP handlers and JSON error responses.
9. Logout/clear HTTP handlers and authorization tests.
10. List/create/join HTTP handlers and supplied API passoff tests.
11. Malformed-request handling and service coverage gaps found in testing.
12. Complete HTTP lifecycle regression tests and final submission preparation.

Day 1, commit 1 (October 12): added UserData, AuthData, and GameData records in
`shared/src/main/java/model`. Implemented the planned storage interface in
`server/src/main/java/dataaccess/DataAccess.java`, with MemoryDataAccess and the
course-provided DataAccessException. Users, tokens, and games have independent
maps per store. Duplicate usernames/tokens cannot overwrite existing records;
game IDs are allocated by the store, new seats are empty, and updates reject
unknown IDs. Logout storage deletes only the requested token; clear removes all
records. Individual storage operations are synchronized, and listGames returns
a separate collection. GameData's nested ChessGame remains mutable; this is not
a deep-copy or multi-operation transaction guarantee.

Implemented interfaces: the three model records and DataAccess methods above.
The service and HTTP interfaces in the Phase 2 contracts remain proposed.
Password hashing and request/authentication validation belong to the forthcoming
services; today's storage code expects service-validated records.

Validation: Java 21 `mvn -B clean verify` passed in all modules: 183 tests,
zero failures/errors/skips (171 existing chess tests and 12 new storage tests).
New tests cover duplicate protection, token isolation, game creation/replacement,
missing-game rejection, collection isolation, clearing, and independent stores.
`git diff --check` passed. No service/HTTP passoff or browser test has run yet;
those layers and course web-client setup remain unimplemented.

Handoff: main; 1 of 12 Phase 3 milestones complete. Next: registration service
with request/result records, BCrypt hashing, UUID authorization, and direct
positive/negative service tests. No autograder or Canvas submission has occurred.

Day 2, commit 2 (October 13): implemented registration in
`server/src/main/java/service/UserService.java`. Request/result records live in
`shared/src/main/java/request/RegisterRequest.java` and
`shared/src/main/java/result/RegisterResult.java`. Missing/blank fields produce
ServiceException.BAD_REQUEST; existing usernames produce ALREADY_TAKEN.
BCrypt salts and hashes passwords before storage. Successful registration stores
UUID AuthData and returns only username/authToken. DuplicateUserException also
handles an insertion conflict after the service's initial username lookup.
Unexpected DataAccessException propagates for the future handler's 500 response.
ServiceException provides business error codes; HTTP translation is not yet built.

Validation: Java 21 `mvn -B clean verify` passed: 199 tests, zero failures/errors/
skips. The 16 registration cases verify hashing, distinct salts/tokens, response
fields, invalid input, duplicate preservation, insertion conflicts, and storage
failures. `git diff --check` passed. No HTTP tests or submission yet.
Handoff: main; 2 of 12 milestones complete. Next: login/logout using the same
UserService and data-access interface. User requested two commits for October 13.

Day 2, commit 3 (October 13): implemented login/logout in UserService. Shared
interfaces are `request/LoginRequest.java`, `request/LogoutRequest.java`, and
`result/LoginResult.java` under `shared/src/main/java`. Login verifies BCrypt
hashes and creates a fresh UUID authorization on each success. Missing/blank
credentials produce BAD_REQUEST; unknown users and wrong passwords produce the
same UNAUTHORIZED error. Passwords and usernames are not trimmed or case-folded.
Logout validates its token, removes only that authorization, and rejects a
missing, unknown, or revoked token. Users, games, and other sessions are preserved.
LogoutRequest's token will come from the authorization header in the HTTP layer.

Validation: Java 21 `mvn -B clean verify` passed: 221 tests, zero failures/errors/
skips. The 22 additional session cases cover successful/repeated login, fresh
UUIDs, malformed credentials, case sensitivity, significant password spaces,
authorization storage failure, independent sessions, invalid/revoked tokens,
and login after logout. Existing registration and chess/storage tests still pass.
`git diff --check` passed. HTTP endpoints and browser/passoff testing remain for
later milestones; this verifies direct service behavior only.

Handoff: main; 3 of 12 Phase 3 milestones complete. Today's two commits satisfy
the extra commit needed for the October 22 deadline; one daily commit from
October 14 through October 22 would finish the remaining nine. Next requested
work: list/create game services and response models with authorization checks
and positive/negative tests. No autograder or Canvas submission has occurred.

Day 3, commit 4 (October 14): implemented list/create game services in
`server/src/main/java/service/GameService.java`. Shared request/result contracts
are `request/ListGamesRequest.java`, `request/CreateGameRequest.java`, and
`result/{ListGamesResult,GameSummary,CreateGameResult}.java` under
`shared/src/main/java`. Both operations require a stored authorization token.
Creation validates gameName, initializes an independent standard ChessGame with
White to move, and leaves both seats empty. Listing includes every game, returns
an empty collection when appropriate, and projects records into summaries that
exclude board/turn state. Nullable seats remain nullable. Invalid authorization
produces UNAUTHORIZED; missing create requests/names produce BAD_REQUEST.

Validation: Java 21 `mvn -B clean verify` passed: 237 tests, zero failures/errors/
skips. The 16 new cases cover standard setup, separate boards/IDs, request and
response shapes, empty/all-game listing, snapshot summaries, bad requests,
authorization ordering, and access after logout. `git diff --check` passed.
Handoff: main; 4 of 12 milestones complete. Next: join game with seat validation.
HTTP handlers and HTTP passoff tests are still pending. User requested two
commits today, so the join milestone follows as a separate tested commit.

Day 3, commit 5 (October 14): added `GameService.joinGame` and the shared
`request/JoinGameRequest.java` record (authToken, playerColor, nullable Integer
 gameID). The authenticated username comes only from AuthData. WHITE/BLACK are
validated exactly; missing/unknown games produce BAD_REQUEST, invalid tokens
produce UNAUTHORIZED, and occupied requested seats produce ALREADY_TAKEN.
Joining preserves the other seat, game name, board, and turn. One account can
join both different seats, matching the supplied course passoff scenario.
The authorization/read/check/update sequence synchronizes on the shared
DataAccess instance to prevent concurrent joins from overwriting each other
in this single-server memory implementation. This is not a distributed SQL
transaction guarantee; revisit storage atomicity when implementing Phase 4.

Validation: Java 21 `mvn -B clean verify` passed: 262 tests, zero failures/errors/
skips. The 25 additional join cases cover both colors, position preservation,
self-play, occupied seats, invalid/revoked tokens, missing/unknown IDs, invalid
colors, authorization ordering, update failures, and concurrent joins through
separate GameService instances sharing the same store. `git diff --check` passed.
These are direct service tests; HTTP endpoints, browser testing, and course HTTP
passoff execution remain pending.

Handoff: main; 5 of 12 Phase 3 milestones complete, with two commits made today
as requested. Seven planned milestones remain before October 22. Next requested
work: clear service and cross-service lifecycle tests, followed by the course
Javalin/static-client setup. No autograder or Canvas submission has occurred.

Day 4, commit 6 (October 16): implemented `service/ClearService.clear()` with
constructor-injected DataAccess. It requires no authorization and delegates to
the store's clear operation, propagating DataAccessException for the future
HTTP handler's 500 response. Six direct service tests exercise clearing multiple
users/sessions/joined games, repeated clearing, rejection of every stale token,
re-registration and new games after clearing, logout-versus-clear behavior, and
isolation between separate stores. These lifecycle tests use the implemented
UserService and GameService together rather than HTTP calls.

Validation: Java 21 `mvn -B -pl server -am test` passed all 268 shared/server tests
(171 chess tests and 97 storage/service tests), with no failures/errors/skips.
`git diff --check` passed. The clear HTTP route is not implemented yet.
Handoff: main; 6 of 12 Phase 3 milestones complete. Next today: course Javalin
bootstrap, static testing page, and server lifecycle checks. No submission made.

Day 4, commit 7 (October 16): copied the course `server/Server.java` and all four
web resources unchanged into the active server module. ServerMain now starts
Javalin on port 8080 and prints the testing-page URL. Server.run(0) supports a
system-assigned test port; Server.stop() releases the listener. Three network
integration tests verify the home page and every asset byte-for-byte, listener
shutdown/port reuse, and independent simultaneous server instances.

Implemented HTTP interface: GET / and the supplied /index.html, /index.js,
/index.css, /favicon.ico resources. Business API routes are not registered yet;
services still have direct tests only. Phase 2's proposed route contracts remain
unchanged. The supplied StandardAPITests will be activated with the game routes
in milestone 10 so the active suite does not falsely imply completed API work.

Validation: Java 21 `mvn -B clean verify` passed all modules and 271 tests with
zero failures/errors/skips. Confirmed Server.java and all web assets match the
starter byte-for-byte. Started the packaged server through ServerMain on 8080;
GET / and all three linked assets returned 200 and exactly matched the packaged
resources. Stopped that temporary server after checking. `git diff --check`
passed. This checks HTTP delivery, not visual browser rendering or API passoff.

Handoff: main; 7 of 12 Phase 3 milestones complete (two commits on October 16).
Five milestones remain before Thursday, October 22. Next: registration/login
HTTP handlers and JSON error responses, followed by logout/clear, game routes,
and final malformed-input/coverage/lifecycle verification. No autograder or
Canvas submission has occurred.

Day 5, commit 8 (October 19): connected `POST /user` and `POST /session` to
UserService through `handler/UserHandler.java`. `handler/HttpJson.java` validates
JSON object syntax and textual field types, deserializes the existing shared
request records, and serializes responses with Gson. Services retain business
validation and storage behavior. Server's default constructor owns a fresh
MemoryDataAccess; an overload accepts DataAccess for testing and future storage.

Implemented interfaces: unauthenticated POST /user accepts RegisterRequest
(username/password/email); POST /session accepts LoginRequest (username/password).
Both return 200 with their existing shared result records (username/authToken).
Service errors map to JSON 400/401/403 with a message starting `Error:`. Storage
and unexpected failures return 500 with a generic JSON message, excluding
internal diagnostics. Responses set application/json; plaintext passwords and
hashes are never returned. Unknown JSON fields are ignored; supplied required
text fields must be strings or null (missing/null/blank fields fail validation).
The Phase 2 design contracts remain the reference for future routes.

Validation: Java 21 `mvn -B clean verify` passed all modules and 304 tests, with
zero failures/errors/skips. The 33 new real HTTP cases cover registration/login,
independent stored tokens, password hashing, duplicate preservation, wrong and
unknown credentials, malformed/non-object/trailing JSON, incompatible types,
missing/blank/null fields, Unicode/escaped credentials, significant password
spaces, storage/runtime failures, and independent default-server storage.
Existing static-resource/lifecycle and direct service/chess tests still pass.
`git diff --check` passed. Full supplied API passoff remains pending until all
routes are implemented; no browser UI or remote grading result is claimed.

Handoff: main; 8 of 12 Phase 3 milestones complete. Exactly one commit requested
and made today. Four milestones remain before October 22: logout/clear routes,
game routes and supplied passoff tests, malformed-request/coverage review, and
final lifecycle/submission preparation. Next: DELETE /session and DELETE /db
using the authorization header and ClearService. No autograder or Canvas
submission has occurred.

Day 5, commit 9 (October 19): added `DELETE /session` in UserHandler and
`DELETE /db` through the new ClearHandler. Logout takes the raw token only from
the authorization header and delegates to UserService.logout; body/query tokens
are ignored. Clear delegates to ClearService with no authorization requirement.
Both successful endpoints return 200 and the JSON object `{}`. Existing exception
handlers return JSON 401 for invalid/revoked sessions and generic JSON 500 for
storage failures. All handlers share the Server constructor's DataAccess instance.

Validation: Java 21 `mvn -B clean verify` passed all modules and 317 tests with
zero failures/errors/skips. Thirteen new HTTP cases verify logout session
isolation, preserved accounts/games, repeated logout rejection, missing/invalid
headers, body/query spoofing, raw-token format, clearing users/sessions/games,
repeatable unauthenticated clear, fresh registration after clear, and storage
failure responses for both endpoints. Game fixtures are seeded through storage
because the game HTTP routes are the next milestone. `git diff --check` passed.
Full supplied API passoff and browser workflow checks remain pending.

Handoff: main; 9 of 12 Phase 3 milestones complete. This is one additional commit
requested today, bringing October 19 to two commits. Three milestones remain
before October 22: game HTTP routes with supplied passoff tests; malformed-request
and service coverage review; final lifecycle/submission preparation. Next: GET,
POST, and PUT /game, including authorization-header handling and gameID parsing.
No autograder or Canvas submission has occurred.

Day 6, commit 10 (October 20): connected GET, POST, and PUT /game through
GameHandler and the existing GameService, using the same DataAccess instance as
the user and clear handlers. GET returns ListGamesResult with game summaries
and no board state. POST accepts gameName and returns CreateGameResult(gameID).
PUT accepts playerColor and gameID and returns an empty JSON object. Protected
requests take the raw authorization header; body/query tokens and supplied
usernames cannot grant access or choose a player identity. All successes use
200; existing exception mapping supplies JSON 400/401/403/500 errors. Game IDs
must be JSON numbers exactly representable as an integer; missing/null IDs are
validated by the service, and strings, fractions, and overflow are rejected.
The shared request/result records and design/phase2/contracts.md document the
interfaces. Copied the supplied StandardAPITests into the active test tree
without changing its contents.

Validation: Java 21 `mvn -B clean verify` passed all modules and 373 tests with
zero failures/errors/skips, including all 22 supplied API tests and 34 new game
HTTP cases. The new cases cover the create/join/list lifecycle, empty results,
response fields, initial/preserved board state, both player seats, occupied-seat
rejection, spoofed/revoked authorization, missing fields, invalid names/colors,
and nonnumeric/fractional/overflowing IDs. Confirmed the active supplied test file
is byte-for-byte identical to starter-code. `git diff --check` passed.

Handoff: main; 10 of 12 Phase 3 milestones complete. One commit requested today.
All required HTTP routes are now connected and the local supplied API suite
passes. Remaining before October 22: malformed-request/service coverage review,
then final lifecycle and submission preparation. No browser workflow check,
remote autograder run, or Canvas submission is claimed for this milestone.
