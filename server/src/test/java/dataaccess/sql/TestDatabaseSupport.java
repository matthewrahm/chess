package dataaccess.sql;

import dataaccess.DatabaseManager;

import java.lang.reflect.Method;
import java.util.Properties;

/**
 * Resets the application tables inside the configured test database.
 * Use a disposable database in db.properties: tests intentionally clear its tables.
 * The grader grants access only to that configured database.
 */
final class TestDatabaseSupport implements AutoCloseable {
    private final Properties original = new Properties();
    private final String databaseName;

    TestDatabaseSupport() throws Exception {
        try (var stream = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (stream == null) {
                throw new IllegalStateException("Copy db.properties.example to db.properties before testing");
            }
            original.load(stream);
        }
        databaseName = original.getProperty("db.name");
        configure(original);
        DatabaseManager.createDatabase();
        resetTables();
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

    private void resetTables() throws Exception {
        try (var connection = DatabaseManager.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE IF EXISTS authorizations");
            statement.executeUpdate("DROP TABLE IF EXISTS games");
            statement.executeUpdate("DROP TABLE IF EXISTS users");
        }
    }

    @Override
    public void close() throws Exception {
        configure(original);
        resetTables();
    }
}
