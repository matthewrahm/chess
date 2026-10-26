package dataaccess;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Loads the course database configuration and opens caller-owned connections. */
public class DatabaseManager {
    private static String databaseName;
    private static String dbUsername;
    private static String dbPassword;
    private static String connectionUrl;

    static {
        loadPropertiesFromResources();
    }

    public static void createDatabase() throws DataAccessException {
        // SQL parameters cannot represent identifiers; quote the configured name.
        String identifier = "`" + databaseName.replace("`", "``") + "`";
        try (var connection = openConnection();
             var statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE IF NOT EXISTS " + identifier);
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to create database", exception);
        }
    }

    /** The caller must close the returned connection, preferably with try-with-resources. */
    public static Connection getConnection() throws DataAccessException {
        try {
            var connection = openConnection();
            try {
                connection.setCatalog(databaseName);
                return connection;
            } catch (SQLException exception) {
                closeAfterFailure(connection, exception);
                throw exception;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to connect to database", exception);
        }
    }

    private static Connection openConnection() throws SQLException {
        return DriverManager.getConnection(connectionUrl, dbUsername, dbPassword);
    }

    private static void closeAfterFailure(Connection connection, SQLException failure) {
        try {
            connection.close();
        } catch (SQLException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private static void loadPropertiesFromResources() {
        var loader = Thread.currentThread().getContextClassLoader();
        try (var stream = loader.getResourceAsStream("db.properties")) {
            if (stream == null) {
                throw new IOException("Missing db.properties in server resources");
            }
            var properties = new Properties();
            properties.load(stream);
            loadProperties(properties);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load database configuration", exception);
        }
    }

    // Preserve the starter signature used by the official database outage test.
    private static void loadProperties(Properties properties) {
        databaseName = properties.getProperty("db.name");
        dbUsername = properties.getProperty("db.user");
        dbPassword = properties.getProperty("db.password");
        String host = properties.getProperty("db.host");
        int port = Integer.parseInt(properties.getProperty("db.port"));
        connectionUrl = String.format("jdbc:mysql://%s:%d", host, port);
    }
}
