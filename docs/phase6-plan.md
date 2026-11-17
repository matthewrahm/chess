# Phase 6: live gameplay plan

Planning baseline: November 17, 2026, `main` at `4396ff4`. This is a proposed
implementation plan; live gameplay is not implemented yet. Milestone 1 is now
implemented: shared protocol classes and serialization tests.

## Sources and starting point

- [Gameplay requirements](https://github.com/softwareconstruction240/softwareconstruction/blob/3b24b24e02a70a4b9c3111e74a7dabaebe009d32/chess/6-gameplay/gameplay.md)
- [Setup](https://github.com/softwareconstruction240/softwareconstruction/blob/3b24b24e02a70a4b9c3111e74a7dabaebe009d32/chess/6-gameplay/getting-started.md)
- [TA tips](https://github.com/softwareconstruction240/softwareconstruction/blob/3b24b24e02a70a4b9c3111e74a7dabaebe009d32/instruction/chess-tips/chess-tips.md#phase-6---gameplay)
- [Quality rubric](https://github.com/softwareconstruction240/softwareconstruction/blob/3b24b24e02a70a4b9c3111e74a7dabaebe009d32/instruction/code-quality-rubric/code-quality-rubric.md)
- Starter revision: `d3ffc6639e2d78d0e88b3fe899936b995d8614e1`. All four local
  Phase 6 starter Java files match that revision byte-for-byte.

Existing HTTP authentication, game creation/listing/joining, SQL persistence,
chess rules, and board rendering provide the foundation. `Server` has no `/ws`
route. `ConsoleClient` currently draws the initial board and tracks only its
perspective, not an active game ID or live state. `GameData` contains a
`ChessGame`; `GameJson` already persists that game's complete JSON. There is no
durable resignation marker. Tyrus dependencies already exist in both modules.

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
- **Server:** register `/ws` with Javalin. A handler parses and validates messages;
  a gameplay service authorizes actions and updates storage; a connection registry
  tracks game subscriptions and delivers messages to the intended clients.
- **Persistence:** add a backward-compatible game-over flag to `ChessGame`, stored
  in the existing JSON, with old records defaulting to false. Keep existing public
  signatures. Confirm copies, equality, and JSON round trips preserve the flag.
  Checkmate/stalemate detection and resignation all prevent subsequent moves.
- **Atomicity:** initially use the same store monitor as HTTP `joinGame` for
  read/check/update operations. Work on a detached game copy so failed writes
  cannot mutate memory storage. Prepare immutable event snapshots only after
  persistence succeeds and enqueue them in commit order. Serialize socket writes;
  never perform blocking network sends while holding the storage monitor.
- **Client transport:** a dedicated communicator owns connection lifecycle and
  JSON. An observer callback delivers typed messages to a gameplay session that
  holds game ID, role, perspective, and the latest server state. Guard state and
  console output against asynchronous callbacks; stale callbacks from a previous
  connection must not redraw a later session.
- **Console:** continue using the existing renderer, with optional selected-square
  and legal-destination highlights. Translate algebraic coordinates into the
  existing 1-based `ChessPosition` rows/columns. Promotion accepts queen, rook,
  bishop, or knight. Reuse `DisplayText` when rendering remote text.

### Proposed wire contract

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

`LOAD_GAME` contains `game` (proposed type: `ChessGame`), `ERROR` contains
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
| 2 / Nov 19 | 3–4: WebSocket endpoint and connecting |
| 3 / Nov 21 | 5–6: moves and terminal-state notifications |
| 4 / Nov 23 | 7–8: leaving and resignation |
| 5 / Nov 28 | 9–10: official integration tests and client transport |
| 6 / Nov 30 | 11–12: live board and move/promotion input |
| 7 / Dec 1 | 13–14: gameplay commands and final verification |

Target December 2–3 for remaining fixes, grading, and the TA demonstration.
Fourteen is the planned milestone count; necessary follow-up fixes may add commits.

| # | Deliverable and verification |
| --- | --- |
| 1 | **Complete Nov 17:** protocol classes, typed messages, and 12 serialization cases; all 183 shared tests pass. |
| 2 | Add durable game-over state and detached copies; verify old JSON, memory isolation, and SQL persistence. |
| 3 | Add `/ws`, parsing, authentication, and subscription registry; test malformed input and cleanup. |
| 4 | Implement CONNECT; test current-state loading, observer access, recipients, and game isolation. |
| 5 | Implement MAKE_MOVE; test legal moves, wrong turn/piece, observer denial, and failed persistence. |
| 6 | Implement terminal-state and check notifications; test checkmate/stalemate and rejected later moves. |
| 7 | Implement LEAVE; test seat release, observer departure, and replacement players. |
| 8 | Implement RESIGN; test player-only authorization, retained subscriptions, restart durability, and repeated resignation. |
| 9 | Activate unchanged official WebSocket tests/helpers; run the server suite in a disposable database and fix integration defects. |
| 10 | Add client communicator/session; test lifecycle, typed callbacks, close/error handling, and stale events. |
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

**Next concrete step:** milestone 2, durable game-over state and detached copies.
A TA appointment has not been scheduled.
