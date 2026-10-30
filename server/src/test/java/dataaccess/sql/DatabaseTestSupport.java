package dataaccess.sql;

import dataaccess.DatabaseManager;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

/** Gives SQL tests an isolated catalog without changing the development database. */
final class DatabaseTestSupport implements AutoCloseable {
    private final Properties original = new Properties();
    private final String databaseName = "chess_test_" + UUID.randomUUID().toString().replace("-", "");

    DatabaseTestSupport() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (stream == null) {
                throw new IllegalStateException("Copy db.properties.example to db.properties before testing");
            }
            original.load(stream);
        }
        configureDatabase(databaseName);
    }

    String databaseName() {
        return databaseName;
    }

    void configureDatabase(String name) throws Exception {
        var properties = new Properties();
        properties.putAll(original);
        properties.setProperty("db.name", name);
        configure(properties);
    }

    void configureUnavailableServer() throws Exception {
        var properties = new Properties();
        properties.putAll(original);
        properties.setProperty("db.port", "100000");
        configure(properties);
    }

    private void configure(Properties properties) throws Exception {
        Method method = DatabaseManager.class.getDeclaredMethod("loadProperties", Properties.class);
        method.setAccessible(true);
        method.invoke(null, properties);
    }

    private Connection adminConnection() throws SQLException {
        String url = "jdbc:mysql://" + original.getProperty("db.host") + ":" + original.getProperty("db.port");
        return DriverManager.getConnection(url, original.getProperty("db.user"), original.getProperty("db.password"));
    }

    @Override
    public void close() throws Exception {
        try (var connection = adminConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS `" + databaseName + "`");
        } finally {
            configure(original);
        }
    }
}
