package tukano.impl;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.IOException;
import java.util.Properties;

public class DatabaseConnectionTest {

    @BeforeAll
    static void registerDriver() {
        try {
            // Explicitly register the PostgreSQL driver
            Driver driver = new org.postgresql.Driver();
            DriverManager.registerDriver(driver);

            // Also try the Class.forName approach
            Class.forName("org.postgresql.Driver");
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println("Failed to register PostgreSQL driver:");
            e.printStackTrace();
        }
    }

    @Test
    void testDatabaseConnection() throws SQLException, IOException {
        // Print classpath to help debug
        System.out.println("Classpath entries:");
        String classpath = System.getProperty("java.class.path");
        for (String path : classpath.split(System.getProperty("path.separator"))) {
            System.out.println(" - " + path);
        }

        // Print available drivers
        System.out.println("\nAvailable JDBC Drivers:");
        DriverManager.drivers().forEach(driver ->
                System.out.println(" - " + driver.getClass().getName()));

        // Load properties file
        Properties props = new Properties();
        props.load(getClass().getClassLoader().getResourceAsStream("db.properties"));

        // Print connection details (without password)
        System.out.println("\nAttempting connection with:");
        System.out.println("URL: " + props.getProperty("connectionString"));
        System.out.println("Username: " + props.getProperty("username"));

        // Try to establish connection
        try (Connection conn = DriverManager.getConnection(
                props.getProperty("connectionString"),
                props.getProperty("username"),
                props.getProperty("password"))) {

            System.out.println("\nConnection successful!");
            System.out.println("Database product name: " + conn.getMetaData().getDatabaseProductName());
            System.out.println("Database product version: " + conn.getMetaData().getDatabaseProductVersion());
        }
    }
}