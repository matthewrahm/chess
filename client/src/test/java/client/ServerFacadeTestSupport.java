package client;

import dataaccess.sql.SqlDataAccess;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import server.Server;

/** Owns the random-port server and disposable-database lifecycle for facade tests. */
abstract class ServerFacadeTestSupport {
    private static Server server;
    static ServerFacade facade;
    static SqlDataAccess storage;

    @BeforeAll
    static void startServer() throws Exception {
        server = new Server();
        var port = server.run(0);
        facade = new ServerFacade(port);
        storage = new SqlDataAccess();
    }

    @BeforeEach
    void clearDatabase() throws Exception {
        facade.clear();
    }

    @AfterAll
    static void stopServer() throws Exception {
        try {
            if (storage != null) {
                storage.clear();
            }
        } finally {
            if (server != null) {
                server.stop();
            }
        }
    }

}
