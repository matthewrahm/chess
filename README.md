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

[Open the current design in presentation mode](https://sequencediagram.org/index.html?presentationMode=readOnly#initialData=C4S2BsFMAIGEAtIGcnQMqQE4DcvQCLIgDmAdtALTQCyAhsMIgO7QBKt8AtgFC0DGwAPaY44EJFLBuAB1qZQfELMnosuTDLkKltFQAldAEygbZ8kIuXBVOC5E3nLu6-nq0Agnz7Ik3Q24AjWiQYV2BaIJDubgB6GOgABUxBaUEQw2gQSSx+UEFSVAAzYUT4YJgAZgBuaH9wyJgUnLyC2uQ+TBAAmEYYJCFMWmIYPnzgQYEAOm5iZIBXaWgAYlJabABPNkhiEH68JaZ4MHtYMQlrCgA+G3UALkSAeTQAFWgYuZDMAB1SAG8PrCrTiQAA00FkKCYwkMYMgnFoIHAAF9uBgcHgrtADKRjFh7phtrtgFhsbjMAAKUbZAAewAAlNxSSZKNcmXi2p8QLQxAAvGAAKTQDwAcplJIItjs9phWJAAI5zZBSAIE2gAa2g8PAxUwwIygpF0BKWVGnFkoACUGghXE4AywHW0mQjKMzIomLRd2gABYAAy+6C-ABEwJQQ0gQduQYAophkph7kEMgSFUqgyjPRjMadxJJ7n6A8HQ0hw5GY3HhInaMn5Yr+unuBJDC6cW7rp67PjCdLybKpcSZbWlQyVZB1dAU3XrLtoKQ5uBwEaRADMEDQeDgkgoZgYUvoHCEYuZ5xdkgssQYnOF6i1HZKJi2QnoIxkiwAELV2Wp-rR6neaQtC2ZL3u2ajsgWgYhj4pZRrG8ZVjW37AA2mYiO61w5uc+b+pBxYweW8HQEmE5DvWKJNjetjeCyBBuJ43goPcwzAAAqp85IrmuDJhB4Xg+DRPENPcNo4tAK5EZsnG0MC3CCeUIG0eE9E+PcbFYDxe5XuAsl0XxKAKR23j3OJAC80BqZgGklFp3CjuO4kzqQgjWDZhkwOhWKuuyL6CCw7jgKqhjrM86oSL+-6AY+BlgU+foVLh0HDGWcGVtA3KBZs4RqhIKExQpmF5j6vrxUWiURrBFZPulY5Bc+oWkA2FFuTRbn3BCW7QgYSDwNAZllN1CSbtuhjku1w0Ms1mI8cpjHQB0Y7EhZ5IWTxHGfGuYJjZ1wTwLC8KInS3G6QxqBTYE5T3P0wgwCtbg6fU8kedNelIJdAyQM2z0ndFVGQF2wBzKulHYHeHq3kZaVzIwzyCNl5BmcMpDNJAMNw+SdI-B857mSxACS+CTIMOKCJwLF4-g6PA6D1xfSpc2qsS7hQ-A5JM4wq20MzqMSGCUnAod928d9Z0PSEb3XdAbPwDxgsNAptOzVdBKfcd-Eea1JEA0Dk0Pl5T59kSWCykg87AGtgLSeunPQ7DEgMlFuutuynLciAfIkSb4DWEI0AGsKQFumD6JPgATDh-zrZbYLW-A3OkBmeUeQVwD3GHhZ81bXO2-HjY4tEcSS17FugLgz5YCeqwtEahSSobIggDXuibHUtBpS9RpOoMVeFIeSDTLMggLMssy0JsBvStASvhvulXLIcxzcE5xJGuoNx2CCcliwQgPY03tTnSEHfNCA+RpQEgi4DUV2LL0s7CFq1rgL50yb+5ItC3TU-DNah6A-YCunSDiDCGPkWAAPCpAACJ9SBU2oh5R8bVkiyGIPQUIqsUAQKgfkAOWZQLB3uAAVnDlBMMSUKqESyAOVYi5PirywPGXKwd8pnEKkQwsJCSxkIIqlShFsaF5XocIRqecZjzEWEsYQuhv4ABlBA7HIAcI4xJuDJxajFe4CQnivBiCEFA0CfgRwtsCTaQ1oQJyYY7Mk9wn7yMfJSMYkBaT2z1jRBBHIsBcl5AKIUopKESlkfIr8U5bKqg1FqHUepfY+L3CaEm5ouhWhtJAO0z5HTOgdkA8CxC8JcJSk+Yik40zmNXknFhKcirsJyeVbh+TqwkSQsI5sDs8HAL+tAGxWRyQBKyEE4cISxwakKf0TIqAtJ7gznuLaO4RmalPOeS885tI61ZHre4oDoAfkML0n8f5IGRRcerdRFSEqkOqXkhC9SpyMJKdmMp2FKllWSpVC5QzkLkTzpNGm6DXrQGYktDOR0lLt3fkJa0WQMjiQCJJSOMlX7y2+apT4VkRA2QAT9VpxlPi9XMkitwmlFn9PslixyBLlmeSdk+dZLFVjM2EG7D6mD9kUvRVkgAjCczhZznliRpYwOlfJDDXNwaIXM5S-TstKqcp5hE5i8vgPyj6jTYHuRaZ2DckJoR0GAHwRASBRqmJ3LzT4kwpmGBHKE9VHVpmGEEMgO+1h4Tap6rfJWH1oD9XgMqhSbiqVyoVYYRl0CcFoUybFX0EqOH4XOTymO-qhUhowncoqEaqnStSrK2NnQBVKs+WvCGMc47YsRsjOO6NMZnlIMQHG+NCZGBJmTfGlNc0APuPNVBUtWbMw5lnOGRqjGQAFmikFF1J7vVnJAPyXa3A1GkASWhjRegiF0WefIvg4VPQRaO66KsgXCyAWqgkWsYFkrcd00gxtTbm1XFHSGNs4bOOZZYkwl0PGu3dnO02z4JR+2DSy0O4cM7Rx7RIYpwrk6pwAzCzOd6QO52bLEeI-kqH0BAKXAcFcUOn0EDXM9mRG6kGbni-g30mhd2gT-REfdRGD3ESPTYuGv4wEESIRRC8l6NFXm5DeB82n4B3pWtKBH96i0aJ3TD5AIgX0gFfIQN9EB311NyR+z9ZaPXfjNH5jGKPgD-oLDT6K1XrPAbsrBx7wZvxWRSxBKQhioMUh-DBJmmXAUOfg6AbCOVRu5bw69-CmHMfjcw0VhDsmPPITw7IvnJ4CMqkq6jQ8lizEgBIaAATB7WFY8o1Rob7j4GjNI6MzxoxvGXfov1WbxP3ALdnYGGJLNWPaXI9LdiqTEicb+p97JAq3vlRVquiBqx4D8alprUNtlm2q-ejrqqIY2PS100bwBxvmoGT1-l4mv1wxmSePRAmShwgAusL18DVnPngK+cy5X6UBqc0GjJLS2WedydyjNtKs2KtAwmkVWFk1Pa5TKq72b3nNmbZu5iHbJt2z08Cr5InhLgrW318jUKetx1U4fDdu66ZS2RbOAlQ7935uZtinHeLrIErshqGOMzXLme9ad31mbruBuwfdm4j3JWcrTU+V7fL3uCs+0Fn74q-vc+MoDj7cHjuw4cz8wwyTICM2ZuSSHpBAWy4EjxrsnApNGlIOATK8nHU6t3m9t24n0cWfsxp+48uoDEh3Rr1zGLNaAxgexleeAuOvw0XOsCYlPhIDBCg0M0dRLckXM5RAS6fDQKoye07h63fTfJQ1tty8DsOiiYaOdqQCgwF+CiNnqEIOFkF6U4L0A06BmB-nRDRdr0lx6OXLIG3sMjeIOlvD+4wDR+E63Yj-FSMbZ7pR-uYjh6DHo4trdgxv7Mbnko+wHupMiG91r7enQBPg+JyUO3ivIBSzPlJmTKQzswCcopxchQn5MBfjx+FWPFYDGnqPnTBJod7pm20oz3yWdmd+npys3BCQVs2XmMwijuwORy3c1CylXCyfB82oWi381i3L1uUrw805y8woUi2QPnREAC1r3i1oyn1EDHBEFoGkGkDED4A20yxODKTUTczywKyKzeEMACFqzQnq2fTmigDkBawcXa2aTzTaT4AEIpAmjp3UxelbUkMpiHRl1BQJB11Lgj0R3NxaDXQf0x1l21yk0d302HS3lUN1w0JD2dHXVkJOgMNwCMJh3sxULhHMIXADywB0JE0f30JIjUI+k-zVkJzaST21hkM6yfBCJgVELcXTyYzNCzz9g9jz0PkLw6xgOr1SNQiF0KgyJRBX1XmTm4zh2gGFAlCZx5A2wG3lxEBKFeSIkEFqhnEnBAGVkmB+HcAkwChqnWAoEz02FdTSnACQAlBNj4g+iowogQ0LmQybzLl1Fbyrnb1OAoO7z3jMOwCUzfyowHgSzo3ILkFn2ngXwYMXmcg4y93MyKIiBHT4y3yrQkIoPRmP0vi3Tkwv3vk2Nv3vy8L0Jt0OO-jf10wJ2-zWXO18mtxen-2O3COs2QTswgL2SgMfRgKwMjWe1wOQz8zoTQO4OyPKVRNTQQPuCQKUwIJngYWIJ2PEWVlSyJGgAAHFLZUATjssHsnx6TCs3hLCfhyjKtUcassiYT2kiRGTQwhCaR6QU83FuteT+saohtxRaT+hRTkBxsVdgM1cU8NYxBlSmSukRSmTltCUqczcKiq4hAttjw5k9tCD4ijsE9gDGdTSGVbtWdoC2TsIU0wsalxdyjJdBSMDhdw1RciSY1nSBcpdQcn8fkd9GB1SYNNSlCnCR0RIMhZTkdNhVdLdvC-jSdwh8VrxgSxCqticzI8zW5ydrxKcesadSUwjeDvIwSWBqU-SbtIC3TkSPTftsD0T00JcIyAzE1K8Rcez-s+zWyc0ZCZc-idTgAVS9V1djDlCR1D1OhIB1C3DLDPDriMcbC6ZRgFxIABByN28VSNJRo0gzxLRNg+jFzgUgjW1BBDzjysMa4zy7pc0NZ4RFgxwdUGTLYNIfYVS0A5hOB4RMB1gfhyRLD8YwR55FooMwRLR+A1QLINpflLZhRLZpDACTtgDpEDTQwL0vZoKmSH0XMGynwXYvEPZP0fY-YagTQdN5c8c3CaDR48A1xfBi8jkMigwtzIwABtX4GC-AOCpfNCm9ZCvgVCxCjC4ELC4EJEMESYVSgAXXQKHJ+z4oEtuGEtEvEuOEkuMSInABQuMvXEsMUsgGUugFUsmA0tOOXlXzEKuNBVlCPUGMXC3LBCXmgAACsPhvZ5M6DDzMAAByVALcto0gfACUPynXQwBuTYLcvcUrVdGoEmMAc-OAXVJAFU0dOzQoZITgHK3PVdSAaYfIvAQotxaMO02fNpHSpkoSjSn4alQQLwBYcQDIEIegH5eCyACyvcaS2S-tWshcaYSYguJDYuVDZveYyuE8nDOk+c7vSAXvPAFuNuEjMTbuXucfGjSfUeJU6wNarTY4wapy84tfS4n3TfbGWMnqEoWctayTF46+HKy-B+G-FTaw6cuQ-4mAQEj-IsjWX-aMqEh0hrWdGzFBcAv-V0qI909nJ8Ak706NEkrEvAIg3EivH7dG+An0sUTElA7EikqXEg5YaQQGGgmAWABmGAAqlkpgmAzRF4Lky2Hk00vk1XAxKyy2cvSi1tRmlU8UtrSU6IxPGqTQs08jKovAIwdxToN9bxEUH4YbBmhaSAFUtU1XYPTC7C40zUbkCJN1RI40UgU0eJS0YG20e0NJbilGkvY5UcsXIiOpV5QLfGwqCCN20MgpUiN5KXUQjWWIlU8kLW1BXWoOlbccdM0+C0lLK03bKtfbO06E4Ws7C7Fs8MqEnitzEctEscnnfs72wM324M-24m3nXra7Sc3CgG2w35RXCHDUu84WZcreVM2WjbFHLM-6iE5u8sgs7SMG8zEsxgEnKdfMys7Sas6nEla8aGvgp0vnZnJGtIrsouwkmusuzS77Sur0om6NWuuNWvasuopeyPEQAW4Ebba0i8WnXCoU9ZTZcbfO52o5P24u92wOhpA+8DV23+gOz2oOhu1pZgl3Sw7FJGFgBAHwCOjGAocIHEOQXq8IcwATc+dBmoAAdSX01CkyKBaP6GlyHv3NFstlIoUpvUsI7rVi7raQGLgf-OBA0iYF7zSh5RAFTHksgHxigoMtYvAF8sWQNtoZMvoezN+MBs3BICRgyFEoCP0mdzVWUZXvZCjuJBjs9jNlEvIrbFTz4OovpVoq9i-Wz39gLq9AyNEsAaTTsctnxjyLOM91ut+jcpHW0d3n4dqFtVGWcgCsECyByvTxKB9g2sXWi3oBirfCj3BDMvWE4qZMKvkDSlGUWRqEcSJEetSdUIRHIFlR1SkQ+impERmobyWrQxbyWtfLgEZrYZgAbh72ie2sH30mH32rHypsSzIO0aZstiBvJJKBOOqo8daS8a3luMetbt3xEHDqGfeuk1eK+o+Ovy+Jkb3Of0kQBN-lBu+QMxASbIocc3bIAMgbwphtAPhrQUhs3psfZEJq51DKxrJpxpxMHMPvxLgJeeJrebJNxsmKpOWGSYXHBP5BCfIGZtGpUVZq7ISBYm0W5MB15o1IMXYuScwFgCfOEAkYEfwCFuMfZH8qhbFr1nsQlMMbq2JYiJloTvIAVsoNEnl1Mfdj9g1sVMhayBjqQnjNjmzk2iSawBxafkwHxfxjjrCVNvvnNuiUtutpQ1trBWSQdqdCds7NRvuRDOJv-quQceHN+ZwNSj1aKRDoORBOCZ5eoe5dIF5anCld7vNOzgftTr3D6MztpdBJzv7M-s1Zdp3oxpe33rxorrFSrpAb3onNryjJ8Kev5bjgYdUaYfh3Dx5qrn7o1K2abuxxnorJRXx0ObUaJynrLLzdHuNsXsyeXvrK9ezvBNzvXoFT9ZcxgMDZPuDejdDa0qPp1dPpDYokvqDomtEf4fxldYrSfvEcmWFexdxfrlGSib7zwb0FxnYMvw2WkXcFgAAGlPWfUTn36g6W3A5t6jXezalEJ9Xu3vntXq7o1TWyJIypzTmYzFcI6DGVHToU2wVRIYGUdlHB6W0mncdUUi2Hy-GzJ3zZ6C2qyLUYHr792GdD3Pxj2Hmv7C7z2S6XlwGDWgyHkO3CJH3g6moX2NYsgkAnQBAcrMWUn76dQyqg63UDzhBjbXk3U+rpxUBqpqxNhOq+Bur-DNHKUTn-IMoQo4YT3hUA3io+3uUePaosoco8PK6SpI3o0FPMp6olVuRrBaO52xWZkV213oxyGNYFg6gPoCqoOAK3AaGCWgNGBJhAMfhLDJhRqLL8XJg77LLLZvOjbklD59PRXjRUA3wt3d2zOJ6xJpBLPDBrOQO7PhG3PBrPPuanPAN+H-PJHfPgRsuB1n3G7X3jJYvo7qGLPUF4ujakz7qCR2LqJoPW4GPb4cn+g8ngR8Zs3iuYu4uv2jngjFdk9hP-pk8pbgDYj9wGrEjyr88a8t6tWq9w4VPylcjrr3HXL7q1IcqJliqSZJY83fKgndvSqJDRUxQabgAYqkhkB-db4o88BOP8XZwpLBB0Gw97Q3cYrZR-KjyHdLklRUAbV7U5oyhK0eh5M3PKbKmZj5q5iMNFia5bWmnVihN2n24unyMtjDrdiyCkeCqLrZ4xm3GXL19iiZnt85nGBg932b0SgKudGlnz4PrZM1mr9lM78uvgOtMQb-5wPLWIbZcoba23FYa4SEb7nzn5uXbnnjXEC8DSSYsKavmgGZeL3iT5fsbCCcSKIgA).

Editable source: [server-design.uml](design/phase2/server-design.uml).
This is an in-progress design; see [Phase 2 progress](notes.md#phase-2-plan-and-progress) for completed endpoints.
