package client.gameplay;

import client.ServerException;
import websocket.commands.UserGameCommand;
import websocket.messages.ServerMessage;

/** One connection; closing it does not leave the game or release the player's seat. */
public interface GameplayTransport extends AutoCloseable {
    void send(UserGameCommand command) throws ServerException;

    @Override
    void close();

    interface Listener {
        void onMessage(ServerMessage message);

        void onDisconnect(String reason);
    }

    @FunctionalInterface
    interface Factory {
        GameplayTransport open(Listener listener) throws ServerException;
    }
}
