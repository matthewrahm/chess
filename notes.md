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

Progress: 10 of 10 planned Phase 1 commits on main. Core rules and both extra-credit
moves are implemented and pass local checks. Later phases remain at their
starter state. No grading submission made yet; the next step is to submit the
current GitHub revision through the course autograder and review its result.
