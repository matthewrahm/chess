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

[Open the current design in presentation mode](https://sequencediagram.org/index.html?presentationMode=readOnly#initialData=C4S2BsFMAIGEAtIGcnQMqQE4DcvQCLIgDmAdtALTQCyAhsMIgO7QBKt8AtgFC0DGwAPaY44EJFLBuAB1qZQfELMnosuTDLkKltFQAldAEygbZ8kIuXBVOC5E3nLu6-nq0Agnz7Ik3Q24AjWiQYV2BaIJDubgB6GOgABUxBaUEQw2gQSSx+UEFSVAAzYUT4YJgAZgBuaH9wyJgUnLyC2uQ+TBAAmEYYJCFMWmIYPnzgQYEAOm5iZIBXaWgAYlJabABPNkhiEH68JaZ4MHtYMQlrCgA+G3UALkSAeTQAFWgYuZDMAB1SAG8PrCrTiQAA00FkKCYwkMYMgnFoIHAAF9uBgcHgrtADKRjFh7phtrtgFhsbjMAAKUbZAAewAAlNxSSZKNcmXi2p8QLQxAAvGAAKTQDwAcplJIItjs9phWJAAI5zZBSAIE2gAa2g8PAxUwwIygpFjKMzIomLRd2gABYAAzW6C-ABEwJQQ0gDtuDoAophkph7kEMgSFUqHSjzRjMadxJJ7ja7Y7nUhXe6vT7hP7aIH5Yr+qHuBJDEacSbrua7PjCdLybKpcSZdmlQyVZB1ZrdkgssRoADMEDQeDgkgoZgYdASnCEeBUWo7JRMWy-dBGMkWAAhTOy4P9T3U7zSFpFslz0tqdlx+1OnzJj3e30ZrNb4B58MiU3XKPnWO2i+J6+pu-QAG0BBjmT4ogW062N4LIEG4njeCg9zDMAACqnzkj2fYMmEHheD4ME4Q09yFFkGQ9oBmyYbQwLcIR5THrB4TwT49xoVgOFjiIpBzOAU44cxKAMWW3isZ8HElNxvHcM2rZsZgHG7NApCCNYklTsJMBvlixrssuggsO44CqoY6zPOqEg7nuB4LkJp6LjaFQ-lewwpre6bQNyxmbOEaoSM+dkMR+MZWtajkJs5bo3mmi6eS2JlLuZpB5hBGkwRp9xlEg8AJIOw6GOSEJDtCDKpZi-F4Yh0AdC2xJyeSck4Rhnx9mChV5QYWWwvCiJ0thcEVagZWBOU9z9MIMANW4tHDSEDHlQhSCjQMkCFvN+FaelwGQMAcy9pB2CzmaM4idAwykM0kDPIIvmkOSdLdh2pBdihKEAJL4JMgw4oInAve9d37Yd1xrZV1X0JA7hzIw5KQ4wjW0FD8BXTdYJUcCvXTUxA0ETNkBLeN0Cw-AOGYxE9FaSDi3QGNBKrf1C22VBeNbTte2lfOOmLjWRJYLKSA8cATWAtR-YI4wyMSAyNkc8W7KctyIB8lt-PgNYQjQAawqHiaR3oouABM37-M1ItgmLSPXRIYYBVpQXAPchvxmjouIxLpDgTi0RxITqvC6AuBLlgnBZPQID5GOhSSjzIggJHuibHUtAedjTSDC00CFJOSDTLMggLMssy0Js3PStTAyutAWC+sshzHNwynEmO6g3HYIJ0SE9z4LtnYeaQCe42O0jNGH5ARIIuA1GNiy9Epwhahn4D6dM7eaUNWMLfjgzDBnk67fYlOMwdJ16SwlOWZA+4j4D0FaQu9zSMksjEODjG4Qt5+X-k2sRieev3AArEbS8LoXJRQAlkOsqxwDUwClXYQ-k9aBTOMFQB8ZgFJlAf+dyEDhbQM+M3OBmBkqexmPMRYSxhC6G3gAGUEDscgBwjjEm4HbNKdl7gJCeK8GIIQUAjx+MbYWwJWq5WhNbRBMsyT3EXvQhclIxiQFpFLTmME74ciwFyXkAohSiggRKWh9DNygWkqqDUWodR6g1jo7+r5dYWnPOFEBkUsGLiAiBEM4jm622QfbEKaDfyYLcq4zMW1HzEMLNLX+R9mYyKyOSAxWQjGNhMS2Mx7Ye7O04gOSE0Jr6r1ZJze4J9oDrkMEk7cu4L7WRURtdhfinJONctFe8oTQIIK8ZGHxX5-ERSaQBNxDZcwe0LKVYG9MWKnW2nVZ2fV17rTGfUEaGdSIPTwAESiJsaIrzmuMyqk1whZLUqTASg07HlmgPspOEkeJThkhqS5mRUBHPZgU2Wi5ikoVWIjYQisVof2qW8w+Z5rQAEYGkYOcUE+4cwvmMB+XyQw7Sf6iGjL4m0YLHEQr6e5GF5t4UrXCXkthTN76iJHHQYAfBEBIAKmS0cPZJhtWKik1sTKRy1EEMgWe1h4SUvgEuRAZdxoZEyvAIlt9CkCpXBc2F8B8WGH+VfaWdjgUYvQX+KF3ZZXyqRbY98XSQpqoCZC5pWq8WdARYS0ZLcTpnQum7O6D0e5-Q+l9QwP0XUA2tZTe4YNiRExhojeGrtLakFRpsyAGMD5rzJh3IVBIlKQAMkGtwNQH7IFPGOXoIheEdnyL4bZFNdlUxpitY52NakkpZrtUg4rJEmHuAk0gfMBZC17KbDyIabrKMBfWuWGiFZKwJCrNWEpNY2KBQbI2zszZdqtvtZFdsHbTojbO8Wobhle3iIZSBocA51mDqsdOghI5NsyHHPutQ3DJwZqnUO4dM6ImzqQvO5DC6bDPTTCuhCa5MPsA3RozcNJt1xp3buT1e790WbNO96cx4TyFdPQVyldTcgXkvUmDQdlzMql+7ej7wB73LQzSt0SinwGlWfSpn9a0vO0m8++j8hgvyo1ZJVNSVWLlQeCjVpqcHtrwbA6KuqkGooAUA412LFz8agTAxBhDCUvvzksWYkAJDQAMXnawjC66sM453T01DPTPE9G8XN-DtUWvvaQe45s3YLtfK8qR0AZFabkVSYkSiJ19sXMZTtcKrPp0QJmPAeiNN0K0+UwWdnQ09qPGck6rmobxIi1DKLTZTH+blYFkeS5Q2PLbHwiD45OD7nWHWpzDapX6Rlea35CrqMAvi-pw1PHAmmtxd8i1BLPGLoNeitrJqAKdYC-Vq1x1NILLfhM5CAaYvduI-M1+RFlk4iy-C6zFEsv2cLTGk59wibiS4jcxbglSPnMO9e65Uk7mExTQcxSzyJsMTUR8yz9XFVf2VVE1Vg2pPQve5a3reqUWfla5i3jw3Ac9fzJ7b1xb7iGEgFAf1iNyTzclqdwaU2VsEk4OPRopBwDeUFbyqlPc6s8ms5h8me2BqI+R9tMt0aEvMwJKzWtAGm54GAyvDhw7M09iQGCZ+zozZre5NAlSiAc0+BHs+ujaj2c1u85V9kfqYBwjK1YkUytUgFBgL8FE32bjskdvaYHomwfm6N7DwssRt2+3bf7HoQcQ7HtPal6wsdK5gBl1eg5-Bb1DzTrlgjz7c7KffeF4gWn43fuir+uuXOCciF56Bgg4GuyzcRlkpHKOIa5-g5ASeQgkMwBQ-PQoi8mDLwHkWnDJby74d3gSLHh9znFNY1Uq+ivJUPxSMxxu3eaPeZa9xiH7XwHZAE3JghwnLfeLE9ACf6qp-YJn7J-BeAFObqU2+wYmxTgthELQaQ0gxB8E2zp5hemfuLnwIZ4zpmYiGACA51Rkq+BQDkO5hRXnIkbVmZv8T8vVns6cN4qof8KRZlpszscclk8cCcPJeJ1sct80adZoG84CqYkDcA6ZG8cZoM2c4RkDJdToRYC168ICJk8Dmdi0iDY0SD8cA5yChdMD8lX59stoWD6DCDzsTplc2ZwCfMKwOdVd6NnMNdK5StgBNhNY9d81DdjcON79l14xF9Oll8bcUQU9m47YQNiD7hhQJRKdNtgskcRASh3F+hAJBB4pFIQIQBaZJgfh3BR4jI4p1gKAtc5D40YBuQkAJR+Y8IVpn0IIHcfZd0XdA5dR3dcsT1RAT9z1IMeDx40Nw8c4yEC5D8ki5B49t4f0b9-0VJAMecJtDCmCwNOgIMQC5BHVi9S8UgBUK854Mia869iDsMcDN4K4CMiMWcolO8KMasR8Dw+8GNwQmNn5h9i1Pta0TcXxxMelGkwEN9d1BN5MF8P8l8wdV9JM1jpNN80Nt8RBd87dohI9yFaYNMiRoAABxSgpPW-HxYlC0e44zN4UXSAH4MwloWzOdWjG2NXRcMQfoR450P-GkekCQpXOKNAxWcwuKULcUW48EygqLdHQEuLEsIA6RIkCE5AeJAkjEwZGE27P43LIQG6ArYOIrLsErMrCrSQqrN7SnP5RrdjXtFrAbSfIbHFaHRFTQ-VZfXktffkxcEbbLMbTdeHRvJCbaObbE9vGNFbEiCXLrRE9OdZbbUNDg7o7gy7A5a7PiBggQ5mI0q5Y7G7TLS0grJ7JmF7SVNkzUhFeYsfNQ8HcU-7M1V0mHF8K3YKMUg4lxAHdkoUi4uUnolzEk50AGaNBAuNdnToSAVg1A74qgro7A7g0YXiSAAQBIyOQkjiAqNIDsAIYnGQsrWAk5DvE6XMqAAs8ORI4sqaa1TaeERYFsKlB4kWDidWQktAOYTgeETAdYH4ckb496MEWuWqVdQCcAfgNUOSFqCg4EYUEWEqEQkExtWM5AFtVWScygnEjEHcuTTRX5ZWAWPLHXYUGoLIb-OYJHJSG5cERc9YPAPsXwRYupG3B0DM90AAbV+CnPwBnL-RXI7QrKXMguETXMgA3OBCRDBEmFQoAF1hTQdgo-yALbhgLQLwLjhYL+xoK+Blz5zvjELIBkLoBULJgML65Sjuc08Ki+ctgOcUDoEMywQG5oAAArD4NWQVK-PMzAAAclQAzNcNIHwAlF4vx0MFjk2AzKyXM3zRqB+jABaLgGpSQEJLLhfkKGSE4G0uHX1xCGmD0LwAMLUU9FkM2FLXUIvFwsAowp+E+UEC8AWHEAyBCHoCplnMgGIqyVIvIqERgEexuWmAiO9h3T9hAH3TdyPULLROsEJNQB90gD9zwEThvXwlgzDyziyNfRyKLlSt7OdAKM10T2KMYsblTyAMqJWy7hqOz0VNzxKDBOAHSo8gCAJyaPL1nlQ2gWrww12ym24LwxgH6Lb0GLxOq1PjmM5K-gmOcwHyfhYyWrYy+1UNNy4wk16UOPuBkxOKE19BE12JQQOtWNDLFA2Lnx322IgiAA).

Editable source: [server-design.uml](design/phase2/server-design.uml).
This is an in-progress design; see [Phase 2 progress](notes.md#phase-2-plan-and-progress) for completed endpoints.
