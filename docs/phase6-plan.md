# Phase 6: live gameplay plan

Planning baseline: November 17, 2026, `main` at `4396ff4`.
Milestones 1–10 are implemented: protocol, durable snapshots, the validated
WebSocket endpoint, player/observer connections, validated saved moves, and
check/checkmate/stalemate notifications with durable game completion, leaving,
and resignation. Official WebSocket pass-offs and the client connection/session
foundation are implemented. Console integration remains for milestones 11–14.
The remaining milestones are planned.

## Sources and starting point

- [Gameplay requirements](https://github.com/softwareconstruction240/softwareconstruction/blob/3b24b24e02a70a4b9c3111e74a7dabaebe009d32/chess/6-gameplay/gameplay.md)
- [Setup](https://github.com/softwareconstruction240/softwareconstruction/blob/3b24b24e02a70a4b9c3111e74a7dabaebe009d32/chess/6-gameplay/getting-started.md)
- [TA tips](https://github.com/softwareconstruction240/softwareconstruction/blob/3b24b24e02a70a4b9c3111e74a7dabaebe009d32/instruction/chess-tips/chess-tips.md#phase-6---gameplay)
- [Quality rubric](https://github.com/softwareconstruction240/softwareconstruction/blob/3b24b24e02a70a4b9c3111e74a7dabaebe009d32/instruction/code-quality-rubric/code-quality-rubric.md)
- Starter revision: `d3ffc6639e2d78d0e88b3fe899936b995d8614e1`. All four local
  Phase 6 starter Java files match that revision byte-for-byte.

At the planning baseline, existing HTTP authentication, game creation/listing/joining, SQL persistence,
chess rules, and board rendering provide the foundation. At that baseline, `Server` had no `/ws`
route. `ConsoleClient` currently draws the initial board and tracks only its
perspective, not an active game ID or live state. `GameData` contains a
`ChessGame`; `GameJson` already persists that game's complete JSON. There was no
durable resignation marker before milestone 2. Tyrus dependencies already exist in both modules.

Phase 5's November 11 verification recorded 710 passing tests. That is historical
evidence, not a Phase 6 result. Remote grading and TA acceptance remain separate.

## Architecture and interfaces

Preserve the existing three modules, HTTP facade, board appearance, and account
flows. Add focused gameplay components rather than putting socket callbacks and
game rules into `ConsoleClient` or `Server`.

- **Shared protocol (implemented):** supplied base classes and typed move/load/
  error/notification classes are in `shared/src/main/java/websocket`. The supplied
  base classes are unchanged. `ProtocolSerializationTests` checks the wire fields,
  payload separation, promotion choices, and JSON round trips.
- **Server (all four gameplay commands implemented):** `/ws` is registered with Javalin.
  `GameplayHandler` validates commands through `GameCommandReader`, and
  `GameplayService` authenticates each command against current storage.
  `GameConnections` binds each socket to one username/game and queues ordered
  messages through `GameConnection`. CONNECT loads a detached saved game and
  announces the stored player color or observer role to other game subscribers.
  Repeated CONNECT refreshes state without another announcement. Disconnect
  removes subscriptions while retaining seats; reconnect does not require HTTP
  rejoin. MAKE_MOVE validates the subscribed player, ownership, turn, legal move,
  and saved end state under the HTTP join storage lock. A successful save sends
  LOAD_GAME to all game subscribers and a named move notice to everyone except
  the requester. Check/checkmate/stalemate then produces one status notice to
  everyone in the game, including the mover. LEAVE clears the stored player seat, unsubscribes the departing socket, and
  notifies only remaining subscribers. Observer departure does not write storage.
  Failed persistence retains the seat and subscription for retry. RESIGN validates
  current player ownership, persists game over, and notifies every subscriber once
  while retaining seats and subscriptions; observers and ended games are rejected.
- **Persistence (implemented foundation):** `ChessGame.endGame()` records a
  `gameOver` flag in the existing JSON; `isGameOver()` reads it and `makeMove()`
  rejects further moves once set. Old JSON defaults to false. `ChessGame.copy()`
  and `GameData.copy()` preserve board, turn, special-move history, and end state.
  Memory storage now copies on reads/writes, matching SQL isolation, and
  `LoadGameMessage` captures a detached snapshot. The resignation service
  invokes `endGame()` before persisting. MAKE_MOVE now marks
  checkmate/stalemate ended before saving; later moves are rejected after reload
  as well as during the current session. Seats remain occupied.
- **Atomicity:** initially use the same store monitor as HTTP `joinGame` for
  read/check/update operations. Work on a detached game copy so failed writes
  cannot mutate memory storage. Prepare immutable event snapshots only after
  persistence succeeds and enqueue them in commit order. Serialize socket writes;
  never perform blocking network sends while holding the storage monitor.
- **Client transport/session (implemented):** `client.gameplay.WebSocketCommunicator`
  implements `GameplayTransport`, owns a JDK HTTP/WebSocket connection to `/ws`,
  bounds connection/write waits, serializes writes, assembles fragmented text,
  and decodes typed messages. Invalid/oversized/binary messages close the connection
  with a safe error. Closing transport retains player seats; it never sends LEAVE.
  `GameplaySession` owns game ID, role, perspective, and detached state snapshots.
  Each connection attempt has a generation; obsolete messages/errors/close events
  cannot update a reconnected or closed session. Console integration is still pending.
- **Console:** continue using the existing renderer, with optional selected-square
  and legal-destination highlights. Translate algebraic coordinates into the
  existing 1-based `ChessPosition` rows/columns. Promotion accepts queen, rook,
  bishop, or knight. Reuse `DisplayText` when rendering remote text.

### Wire contract (all four commands implemented)

Every command carries `commandType`, `authToken`, and integer `gameID`.
`MAKE_MOVE` also carries `ChessMove move`. Authenticate each command against
current storage and check the session's game subscription and player identity.
Observers may connect, highlight locally, and leave; they cannot move or resign.

| Command | Successful behavior |
| --- | --- |
| `CONNECT` | Send `LOAD_GAME` to requester; notify other subscribers of username and player color or observer role. |
| `MAKE_MOVE` | Validate ownership, turn, move, and game status; persist; send `LOAD_GAME` to everyone and move notification to others. Send check/checkmate/stalemate notifications to everyone when applicable. |
| `LEAVE` | Clear the player's seat if applicable, persist, unsubscribe, and notify others. Observers unsubscribe without changing seats. |
| `RESIGN` | Persist game over and notify everyone; retain seats and subscriptions. Reject observers and already-finished games. |

`LOAD_GAME` contains `game` (implemented type: `ChessGame`), `ERROR` contains
`errorMessage` beginning with `Error:`, and `NOTIFICATION` contains `message`.
Only the appropriate payload field is present. Invalid actions send an error to
the requester only, with no mutation or broadcasts. Preserve the expected message
counts; do not invent extra acknowledgement messages.

Players use HTTP join before connecting. Observers connect directly without HTTP
join. Enter the game view after receiving `LOAD_GAME`, and redraw that actual
state. `leave` returns to the lobby; replace Phase 5's seat-preserving `back`
behavior with an explicit leave action. Resign requires confirmation and retains
the game view. Unexpected transport loss closes subscriptions without silently
forfeiting or releasing seats. Define an explicit reconnect path for a previously
joined player, including HTTP-join-success/WebSocket-connect-failure recovery.

## Implementation milestones

The agreed target is **14 meaningful implementation/test commits across seven
distinct workdays**, normally two completed milestones per day. Spread those
workdays across roughly two weeks, ahead of the December 4 assignment
deadline. Commit and push each milestone when its work and checks are complete.
These are planned work sessions, not completed commits or scheduled automation.

The course requires at least 12 GitHub commits spread over the assignment period.
Its Phase 6 rubric does not specify a numeric day minimum. The public autograder's
[commit-verification documentation](https://github.com/softwareconstruction240/autograder/blob/ef1910bf7960de637c6f65373f21c29af4ebf35b/docs/git-commit-verification.md)
describes an original baseline of three distinct days and notes that thresholds
can vary by phase. The deployed Phase 6 day setting has not been verified. Seven
workdays is our planning target, not a claim about that setting. The official
[Fall 2026 schedule](https://github.com/softwareconstruction240/softwareconstruction/blob/3b24b24e02a70a4b9c3111e74a7dabaebe009d32/schedule/schedule.md)
lists **Friday, December 4, 2026** as the Phase 6 due date. It does not specify a
cutoff time; no section-specific LMS override has been checked. The following
dates are work targets, not automatically scheduled sessions.

| Workday / target date | Planned commits |
| --- | --- |
| 1 / Nov 17 | 1–2: protocol and durable game state |
| 2 / Nov 18 | 3–4: WebSocket endpoint and connecting |
| 3 / Nov 19 | 5–6: moves and terminal-state notifications |
| 4 / Nov 20 | 7–8: leaving and resignation |
| 5 / Nov 24 | 9–10: official integration tests and client transport |
| 6 / Nov 30 | 11–12: live board and move/promotion input |
| 7 / Dec 1 | 13–14: gameplay commands and final verification |

Target December 2–3 for remaining fixes, grading, and the TA demonstration.
Fourteen is the planned milestone count; necessary follow-up fixes may add commits.

| # | Deliverable and verification |
| --- | --- |
| 1 | **Complete Nov 17:** protocol classes, typed messages, and 12 serialization cases; all 183 shared tests pass. |
| 2 | **Complete Nov 17:** durable game-over state, detached copies, and 15 lifecycle/isolation tests; full suite 737 passing. |
| 3 | **Complete Nov 18:** `/ws`, strict parsing, current-token authorization, connection tracking, ordered asynchronous replies, and 27 network tests. |
| 4 | **Complete Nov 18:** CONNECT, saved-state loading, player/observer roles, game isolation, identity binding, and reconnects; 13 additional network tests. |
| 5 | **Complete Nov 19:** MAKE_MOVE, strict move parsing, ownership/turn/legal-move validation, persistence, scoped ordered broadcasts, failed-save recovery, and competing-move protection. |
| 6 | **Complete Nov 19:** check/checkmate/stalemate notifications, durable completion, reconnect/reload rejection, all four promotions, and failed terminal-save isolation. |
| 7 | **Complete Nov 20:** LEAVE, player seat release, observer unsubscribe, replacement players, failed-save retry, authorization, ended-game preservation, and SQL/server restart persistence. |
| 8 | **Complete Nov 20:** RESIGN, player-only authorization on either turn, retained seats/subscriptions, scoped notification counts, failed-save retry, repeat/move rejection, SQL/server restart durability, and competing resignations. |
| 9 | **Complete Nov 24:** activate unchanged official WebSocket tests/helpers; all 18 pass in the full suite and the standalone public grader runner against disposable storage. |
| 10 | **Complete Nov 24:** client communicator and session with typed messages, bounded transport operations, detached snapshots, reconnects, close/error handling, and stale-event protection. |
| 11 | Connect play/observe to the live board; verify both perspectives, current-state reconnect, and two players plus observer. |
| 12 | Add move input and promotion selection; test invalid input and updates arriving during prompts. |
| 13 | Add local highlighting, redraw, help, confirmed resign, and leave; test observer/opponent-piece highlighting and cleanup. |
| 14 | Extend packaged rehearsal, review all graded source, fix remaining defects, and write the Phase 6 pass-off guide. |

Milestone 9 may temporarily expose failures; resolve them before declaring that
milestone complete. Do not disable or rewrite official tests to obtain a pass.

## Completion gates

1. Recheck current official test revisions and keep custom tests separate. The
   inspected `WebSocketTests` starts its own server on an available port; use its
   lifecycle rather than starting a conflicting development instance.
2. Run the full suite against disposable SQL storage, retain existing coverage
   gates, and extend verification tooling for Phase 6's actual grader selection.
3. Test concurrent moves, cross-game isolation, rejected commands, storage
   failures, disconnects, and persistence across real server restarts.
4. Rehearse executable JARs with White, Black, and an observer: legal/illegal
   moves, promotion, all orientations, highlights, leave/rejoin, resignation,
   check/checkmate/stalemate, and closed connections. Inspect actual terminal
   output and server logs, including asynchronous prompt behavior.
5. Review the entire graded production scope for naming, decomposition,
   readability, and package structure; run official Checkstyle separately.
6. Confirm real commit history, submit through the course grader when authorized,
   and complete the TA demonstration. Automated tests (50), functionality (75,
   all-or-nothing), and quality (30) are separate grading categories.

The current course links named "checklist" and "common problems" point to
anchors absent from the current tips file. Use the gameplay rubric and the
available Phase 6 checklist, and recheck those links before pass-off.

**Next concrete step:** milestones 11–12: wire play/observe to the live session and board, then add move and promotion input. A TA appointment has not been scheduled.

## November 20 verification

Rechecked the current course schedule and gameplay specification at revision
`529548a855e8ed3f34debbe877c35f662c366ff3`: Phase 6 is still due December 4,
with no cutoff time stated. All four official Phase 6 starter Java files match
current starter revision `d3ffc6639e2d78d0e88b3fe899936b995d8614e1`.

Milestone 7 adds six real-WebSocket leave cases and one SQL/server restart case.
Course Checkstyle passes across all three modules
(`/tmp/chess-phase6-nov20-leave-quality.log`).
The disposable-database full build passes all 820 tests with no failures or skips;
SQL coverage is 166/171 lines (97.08%), and ServerFacade coverage is 90/90 (100%).
Reports: `/tmp/chess-phase6/nov20-leave`. Official Phase 6 pass-offs remain staged
for milestone 9; these results do not establish full Phase 6 readiness.

Milestone 8 adds six network resignation cases, a SQL/server restart case, and
a concurrent-resignation case. A successful resignation sends only NOTIFICATION
to all game subscribers; no extra LOAD_GAME or acknowledgement was introduced.
Course Checkstyle passes across all production modules
(`/tmp/chess-phase6-nov20-resign-quality.log`).
The final disposable-database build passes all 828 tests with no failures or skips;
SQL coverage remains 166/171 (97.08%) and ServerFacade coverage remains 90/90 (100%).
Reports: `/tmp/chess-phase6/nov20-resign`. Both verification runs removed their
temporary database/accounts and left the development database unchanged.
Today's completed milestones bring the Phase 6 history to 8 of 14 planned commits
across four workdays. Client gameplay, official Phase 6 integration, remote grading,
and the TA demonstration remain for later milestones.

## November 24 verification

Milestone 9 activates the official `passoff.server.WebSocketTests` and
`TestFactory` unchanged. All four starter Java files match current starter
revision `d3ffc6639e2d78d0e88b3fe899936b995d8614e1`; both activated files also
match current public grader revision `ef1910bf7960de637c6f65373f21c29af4ebf35b`.
No server fixes were needed. The full disposable-database build passes 846 tests,
including all 18 official WebSocket cases, with no failures, errors, or skips.
SQL coverage remains 166/171 (97.08%); ServerFacade remains 90/90 (100%).
Course Checkstyle passes across all production modules.

`scripts/verify_tests.py --phase 6 --output <new-directory>` now requires the
WebSocket report and rejects missing/skipped/failed tests, retaining both existing
coverage gates. Missing, successful, and skipped-report paths were checked.
With `--grader-libs` and `--grader-java`, it runs the public JUnit runner selecting
`passoff.server.WebSocketTests`: the public grader compiles only current-phase
pass-offs, whereas this checkout retains earlier tests in the same package.
That independent runner also passes all 18 cases. Phase 6 has no separate unit
coverage rubric in the inspected public grader; retained coverage gates are
regression checks. This is local verification, not a remote grading submission.
Reports: `/tmp/chess-phase6/nov24-official`,
`/tmp/chess-phase6/nov24-official-grader`, and
`/tmp/chess-phase6/nov24-official-quality.log`.

### Client interface for the next milestone

- `new GameplaySession(port, gameID, authToken, role, listener)` establishes the
  session identity; `connect()` opens the socket and sends CONNECT. It returns
  after writing, not after server authorization: only a LOAD_GAME callback moves
  the snapshot from CONNECTING to ACTIVE. Players must HTTP-join first; observers
  connect directly. Connection/write failures throw `ServerException` and also
  notify the listener when the connection becomes unavailable.
- `Listener.onMessage(Snapshot, ServerMessage)` receives the shared typed
  `LoadGameMessage`, `ErrorMessage`, or `NotificationMessage`.
  `onDisconnect(Snapshot, String)` reports connection loss; a disconnected session
  retains its last board and can reconnect without another HTTP join.
  Initial server rejection closes the connection; errors after activation retain
  the session. All callbacks are serialized with session changes. Handlers should
  update state/output promptly and avoid blocking network work inside callbacks.
- `snapshot()` returns game ID, role, perspective, status, and a defensive game
  copy. `close()` permanently invalidates the session and suppresses late
  callbacks without releasing a seat. Close an old session before replacing it.
  Apply `DisplayText.singleLine` to remote text when connecting console output.
- `GameplayTransport.send(UserGameCommand)` supports all four shared wire commands.
  Session-level move/leave/resign APIs and user prompts remain for milestones
  12–13. Preserve server acknowledgement behavior: LEAVE has no success reply;
  RESIGN sends a notification, not LOAD_GAME. Do not infer a server-confirmed
  transition merely because a socket write succeeded.

Milestone 10 adds 28 client cases covering typed decoding, malformed payloads,
all four command wire formats, fragmented/oversized/binary messages, server
closure, connection refusal, lifecycle failures, close-during-connect, stale
callbacks, detached snapshots, and real server player/observer updates and
seat-preserving reconnects. The final disposable-database `--phase 6` build
passes 874 tests, including all 18 official WebSocket cases, with no failures,
errors, or skips. SQL coverage is 166/171 (97.08%); ServerFacade is 90/90 (100%).
Course Checkstyle passes across all production modules. Reports:
`/tmp/chess-phase6/nov24-client` and
`/tmp/chess-phase6/nov24-client-quality.log`.
Both full builds and the standalone runner removed their temporary database and
account. Development data was untouched. The two Nov 24 commits complete
milestones 9–10, bringing Phase 6 to 10 of 14 planned commits across five days
on `main`. Console gameplay, packaged multi-client rehearsal, remote grading,
and the TA demonstration remain outstanding.
