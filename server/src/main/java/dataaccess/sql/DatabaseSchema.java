package dataaccess.sql;

import dataaccess.DataAccessException;
import dataaccess.DatabaseManager;

import java.sql.SQLException;

/** Creates persistent storage without replacing existing tables or records. */
public final class DatabaseSchema {
    private static final String[] CREATE_TABLES = {
            """
            CREATE TABLE IF NOT EXISTS users (
                username VARCHAR(255) PRIMARY KEY,
                password_hash VARCHAR(60) NOT NULL,
                email VARCHAR(255) NOT NULL
            ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin
            """,
            """
            CREATE TABLE IF NOT EXISTS authorizations (
                auth_token VARCHAR(64) PRIMARY KEY,
                username VARCHAR(255) NOT NULL
            ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin
            """,
            """
            CREATE TABLE IF NOT EXISTS games (
                game_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
                white_username VARCHAR(255) DEFAULT NULL,
                black_username VARCHAR(255) DEFAULT NULL,
                game_name VARCHAR(255) NOT NULL,
                game_json TEXT NOT NULL
            ) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_bin
            """
    };

    private DatabaseSchema() {
    }

    public static void initialize() throws DataAccessException {
        DatabaseManager.createDatabase();
        try (var connection = DatabaseManager.getConnection();
             var statement = connection.createStatement()) {
            for (String sql : CREATE_TABLES) {
                statement.executeUpdate(sql);
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Unable to initialize database tables", exception);
        }
    }
}
