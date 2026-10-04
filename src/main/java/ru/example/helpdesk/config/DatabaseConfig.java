package ru.example.helpdesk.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConfig {

    private static final String URL = System.getenv().getOrDefault(
            "DB_URL", "jdbc:postgresql://localhost:5432/helpdesk_db"
    );
    private static final String USER = System.getenv().getOrDefault(
            "DB_USER", "helpdesk_app"
    );
    private static final String PASSWORD = System.getenv("DB_PASSWORD");

    private DatabaseConfig() {}

    public static Connection getConnection() throws SQLException {
        if (PASSWORD == null || PASSWORD.isBlank()) {
            throw new IllegalArgumentException("Не задана переменная окружения DB_PASSWORD");
        }
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}