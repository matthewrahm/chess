# Phase 5 pass-off

Phase 5 pregame functionality is implemented and locally verified as of November
11, 2026. Remote Phase 5 grading and the TA demonstration are still pending.
Live moves and WebSocket updates are Phase 6 work.

## Local verification

These results cover implementation commit `b8df9d9` on `main`.

| Check | Result |
| --- | --- |
| Full Maven suite with restricted disposable MySQL account | 710 passed; no failures, errors, or skipped tests |
| ServerFacade line coverage | 90/90 (100%); required minimum 80% |
| SQL line coverage | 166/171 (97.08%) |
| Public grader's local client-package selection | 186 passed; facade coverage 100% |
| Official Checkstyle over shared, server, and client | No findings |
| Active supplied Java tests and helpers | All 21 match current upstream bytes |
| Executable JAR build and isolated client/server rehearsal | Passed, including server restart and offline errors |

The public grader's local selection excludes the `ui` test package; the full Maven
run includes it. `mvn verify` enforces the facade and SQL coverage gates. It does
not run Checkstyle or submit to the remote grader; those are separate checks.

Reviewed all 62 production Java files, including earlier phases, against naming,
decomposition, readability, and package structure. Console state, input, rendering,
HTTP transport, handlers, services, SQL stores, and chess rules have separate
responsibilities. Shared movement and response validation avoid duplicated logic.
No blocking quality issue was identified; this review does not predict a TA score.
Supplied escape-sequence constants are retained, including unused starter helpers.

Evidence is saved locally under `/tmp/chess-phase5/`: `nov11-auth/`,
`nov11-grader/`, `nov11-packaged-client/`, `nov11-quality.log`,
`nov11-package.log`, and `nov11-provided-tests.json`. Temporary artifacts may be
removed by the OS. Reproduction commands and prerequisites are in the
[README](../README.md#packaged-phase-5-rehearsal).

## Demonstration sequence

Use Java 21 and a running MySQL instance with the configured local database.
Build with `mvn -B -DskipTests package` after running the isolated test verifier.
In separate terminals, run:

```sh
java -jar server/target/server-test-dependencies.jar
java -jar client/target/client-test-dependencies.jar
```

1. Run `help`, then `register` with a new demo account. Confirm registration
   enters the signed-in menu. Passwords should be hidden in an interactive Java
   console; piped-input automation cannot verify that behavior.
2. Run `create`, enter a game name, then `list`. Both seats should still be open.
   Games appear as 1, 2, 3, etc., independent of their stored IDs.
3. Run `play`, try an invalid game number, then retry with the displayed number
   and `white`. The board should have a1 at bottom left, h1 light, labels on all
   sides, and uppercase White pieces. Try `redraw`, `help`, then `back`.
4. Run `logout`, register a second account, and try the occupied White seat.
   Confirm a readable error and retry with Black. The board should have a1 at
   upper right and Black's pieces nearest the player. Use `back`, then `logout`.
5. Register an observer account, run `observe`, and select that full game.
   Confirm White's perspective. Return with `back` and `list`; both player names
   should be unchanged. Test `cancel` at a game-selection prompt.
6. Log out, try a wrong password, then log in correctly. Try an unknown command
   and confirm the menu remains usable. Run `quit` to exit cleanly.
7. Restart the server and log in again: the game and seats should persist.
   With the server stopped, try `list` from a signed-in client and confirm a
   readable connection error. Restart it and retry.

The automated packaged rehearsal checks these main flows against a disposable
database, including stored IDs that differ from displayed numbers. Terminal
appearance and password hiding were inspected November 9; their implementation
has not changed since. Check readability in the terminal used for the TA demo.

## Remaining steps

- Submit the pushed Phase 5 revision to the course autograder and review its
  actual result. Address any reported issues before the demonstration.
- Complete the TA pass-off with the sequence above. Be prepared to explain the
  facade, session state, list-number mapping, and positive/negative tests.
- After Phase 5 is accepted, continue with Phase 6 gameplay. No Phase 6 milestone
  is claimed here.

## Sources checked November 11

- [Phase 5 requirements](https://github.com/softwareconstruction240/softwareconstruction/blob/0fc1a36abbeba9b30854fd223d51fb230581fe16/chess/5-pregame/pregame.md)
- [Code quality rubric](https://github.com/softwareconstruction240/softwareconstruction/blob/0fc1a36abbeba9b30854fd223d51fb230581fe16/instruction/code-quality-rubric/code-quality-rubric.md)
- [Pass-off tips](https://github.com/softwareconstruction240/softwareconstruction/blob/0fc1a36abbeba9b30854fd223d51fb230581fe16/instruction/chess-tips/chess-tips.md#phase-5---pregame)
- Starter revision: `d3ffc6639e2d78d0e88b3fe899936b995d8614e1`.
- Public grader tools revision: `ef1910bf7960de637c6f65373f21c29af4ebf35b`.
