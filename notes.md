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
Next: add king lookup and check detection. Later phases remain at their starter state.
