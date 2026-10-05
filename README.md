# ♕ BYU CS 240 Chess

This project demonstrates mastery of proper software design, client/server architecture, networking using HTTP and WebSocket, database persistence, unit testing, serialization, and security.

## 10k Architecture Overview

The application implements a multiplayer chess server and a command line chess client.

[![Sequence Diagram](10k-architecture.png)](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGEAtIGckCh0AcCGAnUBjEbAO2DnBElIEZVs8RCSzYKrgAmO3AorU6AGVIOAG4jUAEyzAsAIyxIYAERnzFkdKgrFIuaKlaUa0ALQA+ISPE4AXNABWAexDFoAcywBbTcLEizS1VZBSVbbVc9HGgnADNYiN19QzZSDkCrfztHFzdPH1Q-Gwzg9TDEqJj4iuSjdmoMopF7LywAaxgvJ3FC6wCLaFLQyHCdSriEseSm6NMBurT7AFcMaWAYOSdcSRTjTka+7NaO6C6emZK1YdHI-Qma6N6ss3nU4Gpl1ZkNrZwdhfeByy9hwyBA7mIT2KAyGGhuSWi9wuc0sAI49nyMG6ElQQA)

## Modules

The application has three modules.

- **Client**: The command line program used to play a game of chess over the network.
- **Server**: The command line program that listens for network requests from the client and manages users and games.
- **Shared**: Code that is used by both the client and the server. This includes the rules of chess and tracking the state of a game.

## Starter Code

As you create your chess application you will move through specific phases of development. This starts with implementing the moves of chess and finishes with sending game moves over the network between your client and server. You will start each phase by copying course provided [starter-code](starter-code/) for that phase into the source code of the project. Do not copy a phases' starter code before you are ready to begin work on that phase.

## IntelliJ Support

Open the project directory in IntelliJ in order to develop, run, and debug your code using an IDE.

## Maven Support

You can use the following commands to build, test, package, and run your code.

| Command                    | Description                                     |
| -------------------------- | ----------------------------------------------- |
| `mvn compile`              | Builds the code                                 |
| `mvn package`              | Run the tests and build an Uber jar file        |
| `mvn package -DskipTests`  | Build an Uber jar file                          |
| `mvn install`              | Installs the packages into the local repository |
| `mvn test`                 | Run all the tests                               |
| `mvn -pl shared test`      | Run all the shared tests                        |
| `mvn -pl client exec:java` | Build and run the client `Main`                 |
| `mvn -pl server exec:java` | Build and run the server `Main`                 |

These commands are configured by the `pom.xml` (Project Object Model) files. There is a POM file in the root of the project, and one in each of the modules. The root POM defines any global dependencies and references the module POM files.

## Running the program using Java

Once you have compiled your project into an uber jar, you can execute it with the following command.

```sh
java -jar client/target/client-jar-with-dependencies.jar

♕ 240 Chess Client: chess.ChessPiece@7852e922
```

## Phase 2: Chess Server Design

[Open the current design in presentation mode](https://sequencediagram.org/index.html?presentationMode=readOnly#initialData=C4S2BsFMAIGEAtIGcnQMqQE4DcvQCLIgDmAdtALTQCyAhsMIgO7QBKt8AtgFC0DGwAPaY44EJFLBuAB1qZQfELMnosuTDLkKltFQAldAEygbZ8kIuXBVOC5E3nLu6-nq0Agnz7Ik3Q24AjWiQYV2BaIJDubgB6GOgABUxBaUEQw2gQSSx+UEFSVAAzYUT4YJgAZgBuaH9wyJgUnLyC2uQ+TBAAmEYYJCFMWmIYPnzgQYEAOm5iZIBXaWgAYlJabABPNkhiEH68JaZ4MHtYMQlrCgA+G3UALkSAeTQAFWgYuZDMAB1SAG8PrCrTiQAA00FkKCYwkMYMgnFoIHAAF9uBgcHgrtADKRjFh7phtrtgFhsbjMAAKUbZAAewAAlNxSSZKNcmXi2p8QLQxAAvGAAKTQDwAcplJIItjs9phWJAAI5zZBSAIE2gAa2g8PAxUwwIygpFjKMzIomLRd2gABYAAzW6C-ABEwJQQ0gDtuDoAophkph7kEMgSFUqHSjzRjMadxJJ7ja7Y7nUhXe6vT7hP7aIH5Yr+qHuBJDEacSbrua7PjCdLybKpcSZdmlQyVZB1ZrdkgssRoADMEDQeDgkgoZgYdASnCEeBUWo7JRMWy-dBGMkWAAhTOy4P9T3U7zSFpFslz0tqdlx+1OnzJj3e30ZrNb4B58MiU3XKPnWO2i+J6+pu-QAG0BBjmT4ogW062N4LIEG4njeCg9zDMAACqnzkj2fYMmEHheD4ME4Q09yFFkGQ9oBmyYbQwLcIR5THrB4TwT49xoVgOFjiIpBzOAU44cxKAMWW3isZ8HElNxvHcM2rZsZgHG7NApCCNYklTsJMBvlixrssuggsO44CqoY6zPOqEg7nuB4LkJp6LjaFQ-lewwpre6bQNyxmbOEaoSM+dkMR+MZWtajkJs5bo3mmi6eS2JlLuZpB5hBGkwRp9xlEg8AJIOw6GOSEJDtCDKpZi-F4Yh0AdC2xJyeSck4Rhnx9mChV5QYWWwvCiJ0thcEVagZWBOU9z9MIMANW4tHDSEDHlQhSCjQMkCFvN+FaelwGQMAcy9pB2CzmaM4idAwykM0kDPIIvmkOSdLdh2pBdihKEAJL4JMgw4oInAve9d37Yd1xrZV1X0JA7hzIw5KQ4wjW0FD8BXTdYJUcCvXTUxA0ETNkBLeN0Cw-AOGYxE9FaSDi3QGNBKrf1C22VBeNbTte2lfOOmLjWRJYLKSA8cATWAtR-YI4wyMSAyNkc8W7KctyIB8lt-PgNYQjQAawqHiaR3oouABM37-M1ItgmLSPXRIYYBVpQXAPchvxmjouIxLpDgTi0RxITqvC6AuBLlgnBZPQID5GOhSSjzIggJHuibHUtAedjTSDC00CFJOSDTLMggLMssy0Js3PStTAyutAWC+sshzHNwynEmO6g3HYIJ0SE9z4LtnYeaQCe42O0jNGH5ARIIuA1GNiy9Epwhahn4D6dM7eaUNWMLfjgzDBnk67fYlOMwdJ16SwlOWZA+4j4D0FaQu9zSMksjEODjG4Qt5+X-k2sRieev3AArEbS8LoXJRQAlkOsqxwDUwClXYQ-k9aBTOMFQB8ZgFJlAf+dyEDhbQM+M3OBmBkqeyAA).

Editable source: [server-design.uml](design/phase2/server-design.uml).
This is an in-progress design; see [Phase 2 progress](notes.md#phase-2-plan-and-progress) for completed endpoints.
