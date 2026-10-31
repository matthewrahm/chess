package dataaccess.sql;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

/** Owns the database and HTTP server lifecycle for SQL integration tests. */
abstract class TestSqlServerFixture {
    protected TestDatabaseSupport database;
    protected TestSqlServerSupport http;

    @BeforeEach
    void setUp() throws Exception {
        database = new TestDatabaseSupport();
        http = new TestSqlServerSupport();
    }

    @AfterEach
    void tearDown() throws Exception {
        try {
            if (http != null) {
                http.close();
            }
        } finally {
            if (database != null) {
                database.close();
            }
        }
    }

}
