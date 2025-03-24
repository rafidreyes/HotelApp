package com.newhotel.hotelapp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

public class PasswordRecovery {
    public static String getUserEmail(String username) throws SQLException {
        String email = null;
        String query = "SELECT T_RH_Email FROM T_RH_InicioS WHERE T_RH_Usuario = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    email = rs.getString("T_RH_Email");
                }
            }
        }
        return email;
    }

    // Método eliminado: verifySecurityQuestion

    public static String generateTemporaryPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        int length = 12; // Aumentar longitud para mayor seguridad

        for (int i = 0; i < length; i++) {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }

        return sb.toString();
    }

    public static boolean updatePassword(String username, String newPassword) throws SQLException {
        String query = "UPDATE T_RH_InicioS SET T_RH_Contrasenia = ? WHERE T_RH_Usuario = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, newPassword);
            stmt.setString(2, username);

            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        }
    }

    public static String resetPassword(String username) throws SQLException {
        String tempPassword = generateTemporaryPassword();

        if (updatePassword(username, tempPassword)) {
            return tempPassword;
        } else {
            throw new SQLException("No se pudo actualizar la contraseña");
        }
    }
}