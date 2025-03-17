package com.newhotel.hotelapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static final String URL = "jdbc:ucanaccess://C://Users//rr980//Documents//BD_Reservas_Hotel.accdb";
    private static Connection connection; // Conexión persistente
    private static int connectionAttempts = 0;
    private static final int MAX_CONNECTION_ATTEMPTS = 3;

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) { // Verifica si la conexión ya está cerrada
            connectionAttempts++;
            System.out.println("Intento de conexión a la base de datos #" + connectionAttempts);

            try {
                // Verificar que el driver de UCanAccess esté disponible
                try {
                    Class.forName("net.ucanaccess.jdbc.UcanaccessDriver");
                    System.out.println("Driver UCanAccess encontrado correctamente");
                } catch (ClassNotFoundException e) {
                    System.err.println("ERROR CRÍTICO: Driver UCanAccess no encontrado");
                    throw new DatabaseConnectionException("Driver JDBC no encontrado", e);
                }

                // Verificar que la URL de la base de datos sea válida
                System.out.println("Intentando conectar a: " + URL);

                // Intentar establecer la conexión
                connection = DriverManager.getConnection(URL);
                System.out.println("Conexión establecida correctamente");
                connectionAttempts = 0; // Reiniciar contador de intentos
            } catch (SQLException e) {
                System.err.println("Error al conectar a la base de datos: " + e.getMessage());
                e.printStackTrace();

                if (connectionAttempts >= MAX_CONNECTION_ATTEMPTS) {
                    System.err.println("Se alcanzó el número máximo de intentos de conexión. Abortando.");
                    connectionAttempts = 0; // Reiniciar para futuros intentos
                }

                throw new DatabaseConnectionException("Error al conectar a la base de datos: " + e.getMessage(), e);
            }
        } else {
            // Verificar que la conexión sigue siendo válida
            try {
                if (connection.isValid(5)) { // Timeout de 5 segundos
                    System.out.println("Conexión existente válida");
                } else {
                    System.out.println("Conexión existente inválida, creando una nueva");
                    connection.close();
                    connection = DriverManager.getConnection(URL);
                }
            } catch (SQLException e) {
                System.err.println("Error al validar la conexión: " + e.getMessage());
                connection = DriverManager.getConnection(URL);
            }
        }
        return connection;
    }

    public static void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null; // Importante para liberar recursos
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