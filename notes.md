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

Steps 1 through 3 are complete on `main`. King, knight, and rook movement use
separate calculators. `SlidingMoves` follows a direction until the board edge
or an occupied square. Enemy pieces can be captured, but movement stops there.
Rook, bishop, and queen movement share this helper.
`mvn -pl shared test`: 119 tests passed after the rook change, with no failures or errors.
Next: extract bishop movement into its own calculator (step 4).
