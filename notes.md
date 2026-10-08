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
