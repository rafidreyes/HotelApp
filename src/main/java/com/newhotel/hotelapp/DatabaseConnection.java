package com.newhotel.hotelapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:ucanaccess://C://Users//rr980//Documents//BD_Reservas_Hotel.accdb";
    private static Connection connection; // Conexión persistente

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) { // Verifica si la conexión ya está cerrada
            try {
                connection = DriverManager.getConnection(URL);
            } catch (SQLException e) {
                System.err.println("Error al conectar a la base de datos: " + e.getMessage());
                e.printStackTrace();
                throw new DatabaseConnectionException("Error al conectar a la base de datos", e);
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("Conexión cerrada correctamente.");
            } catch (SQLException e) {
                System.err.println("Error al cerrar la conexión: " + e.getMessage());
            }
        }
    }

    public static class DatabaseConnectionException extends SQLException {
        public DatabaseConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}