package server.websocket;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WriteCallback;

import java.util.concurrent.CompletableFuture;

/** Queues asynchronous writes in order without blocking game or storage locks. */
final class GameConnection {
    private final Session session;
    private CompletableFuture<Void> pending = CompletableFuture.completedFuture(null);

    GameConnection(Session session) {
        this.session = session;
    }

    boolean isOpen() {
        return session.isOpen();
    }

    synchronized void send(String text) {
        pending = pending.thenComposeAsync(ignored -> write(text));
        pending.whenComplete((ignored, error) -> {
            if (error != null) {
                session.close();
            }
        });
    }

    private CompletableFuture<Void> write(String text) {
        var result = new CompletableFuture<Void>();
        if (!session.isOpen()) {
            result.completeExceptionally(new IllegalStateException("Connection closed"));
            return result;
        }
        try {
            session.getRemote().sendString(text, new WriteCallback() {
                @Override
                public void writeSuccess() {
                    result.complete(null);
                }

                @Override
                public void writeFailed(Throwable error) {
                    result.completeExceptionally(error);
                }
            });
        } catch (RuntimeException exception) {
            result.completeExceptionally(exception);
        }
        return result;
    }
}
