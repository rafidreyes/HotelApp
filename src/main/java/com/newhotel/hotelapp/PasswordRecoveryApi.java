package com.newhotel.hotelapp;

import okhttp3.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

public class PasswordRecoveryApi {
    private static final String API_URL = "http://localhost:5000/api";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();

    /**
     * Inicia el proceso de recuperación de contraseña
     * @param username Nombre de usuario
     * @return El token generado o null si hay un error
     * @throws Exception Si hay un error en el proceso
     */
    public static String iniciarRecuperacionContrasena(String username) throws Exception {
        try {
            // Obtener el email del usuario usando el método existente
            String email = PasswordRecovery.getUserEmail(username);

            if (email == null || email.isEmpty()) {
                throw new Exception("No se encontró email para el usuario: " + username);
            }

            // Solicitar el token de recuperación a través de la API
            return requestPasswordReset(email);
        } catch (SQLException e) {
            throw new Exception("Error al acceder a la base de datos: " + e.getMessage());
        }
    }

    /**
     * Solicita un token de recuperación de contraseña
     * @param email El correo electrónico del usuario
     * @return El token generado o null si hay un error
     * @throws Exception Si hay un error en la respuesta
     */
    public static String requestPasswordReset(String email) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("email", email);

        RequestBody body = RequestBody.create(requestBody.toJSONString(), JSON);
        Request request = new Request.Builder()
                .url(API_URL + "/request-password-reset")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorMessage = "Error en la solicitud: " + response.code();
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JSONParser parser = new JSONParser();
                        JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);
                        if (jsonResponse.containsKey("error")) {
                            errorMessage = (String) jsonResponse.get("error");
                        }
                    } catch (Exception e) {
                        // Si no podemos parsear la respuesta, usamos el mensaje de error genérico
                    }
                }
                throw new Exception(errorMessage);
            }

            if (response.body() != null) {
                String responseBody = response.body().string();
                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);

                if (jsonResponse.containsKey("token")) {
                    return (String) jsonResponse.get("token");
                } else if (jsonResponse.containsKey("message")) {
                    // Si no hay token pero hay un mensaje de éxito, mostramos el mensaje
                    System.out.println("Mensaje del servidor: " + jsonResponse.get("message"));
                    return (String) jsonResponse.get("token"); // Puede ser null
                } else {
                    throw new Exception("Respuesta del servidor no contiene token");
                }
            } else {
                throw new Exception("Respuesta vacía del servidor");
            }
        }
    }

    /**
     * Valida si un token de recuperación es válido
     * @param token El token a validar
     * @return true si el token es válido, false en caso contrario
     * @throws Exception Si hay un error en la respuesta
     */
    public static boolean validateToken(String token) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("token", token);

        RequestBody body = RequestBody.create(requestBody.toJSONString(), JSON);
        Request request = new Request.Builder()
                .url(API_URL + "/validate-token")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return false;
            }

            if (response.body() != null) {
                String responseBody = response.body().string();
                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);

                return jsonResponse.containsKey("valid") && (boolean) jsonResponse.get("valid");
            } else {
                return false;
            }
        }
    }

    /**
     * Restablece la contraseña del usuario usando el token
     * @param token El token de recuperación
     * @param newPassword La nueva contraseña
     * @return true si se restableció correctamente, false en caso contrario
     * @throws Exception Si hay un error en la respuesta
     */
    public static boolean resetPassword(String token, String newPassword) throws Exception {
        JSONObject requestBody = new JSONObject();
        requestBody.put("token", token);
        requestBody.put("new_password", newPassword);

        RequestBody body = RequestBody.create(requestBody.toJSONString(), JSON);
        Request request = new Request.Builder()
                .url(API_URL + "/reset-password")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorMessage = "Error al restablecer la contraseña: " + response.code();
                if (response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        JSONParser parser = new JSONParser();
                        JSONObject jsonResponse = (JSONObject) parser.parse(responseBody);
                        if (jsonResponse.containsKey("error")) {
                            errorMessage = (String) jsonResponse.get("error");
                        }
                    } catch (Exception e) {
                        // Si no podemos parsear la respuesta, usamos el mensaje de error genérico
                    }
                }
                throw new Exception(errorMessage);
            }

            return true;
        }
    }
}