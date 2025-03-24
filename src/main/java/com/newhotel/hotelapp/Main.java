package com.newhotel.hotelapp;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import netscape.javascript.JSObject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;

public class Main extends Application {

    private static JSBridge bridge;

    public static void main(String[] args) {
        launch(args);
    }

    public static class JSBridge {
        private WebEngine engine;
        private String currentPage = "menu";

        public JSBridge(WebEngine engine) {
            this.engine = engine;
            System.out.println("JSBridge creado");
        }

        public void login(String user, String pass) {
            System.out.println("JSBridge.login llamado con Usuario: " + user);

            try {
                if (validateLogin(user, pass)) {
                    System.out.println("Login Exitoso");
                    loadMenuPage(engine);
                } else {
                    System.out.println("Usuario o contraseña incorrectos");
                    engine.executeScript("document.getElementById('errorMsg').textContent = 'Usuario o contraseña incorrectos'");
                }
            } catch (Exception e) {
                System.out.println("Error en login: " + e.getMessage());
                e.printStackTrace();
                try {
                    engine.executeScript("document.getElementById('errorMsg').textContent = 'Error: " +
                            e.getMessage().replace("'", "\\'") + "'");
                } catch (Exception ex) {
                    System.out.println("Error al mostrar mensaje: " + ex.getMessage());
                }
            }
        }

        private boolean validateLogin(String username, String password) {
            boolean valid = false;
            final String query = "SELECT * FROM T_RH_InicioS WHERE T_RH_Usuario = ? AND T_RH_Contrasenia = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                System.out.println("Validando login para usuario: " + username);
                stmt.setString(1, username);
                stmt.setString(2, password);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Usuario encontrado en la base de datos");
                        valid = true;
                    } else {
                        System.out.println("Usuario o contraseña no encontrados en la base de datos");
                    }
                }
            } catch (SQLException e) {
                System.err.println("Error SQL: " + e.getMessage());
                e.printStackTrace();
            }
            return valid;
        }

        private void loadMenuPage(WebEngine engine) {
            try {
                System.out.println("Cargando menu.html...");
                String menuPath = getClass().getResource("/menu.html").toExternalForm();
                this.currentPage = "menu";
                engine.load(menuPath);
            } catch (Exception e) {
                System.out.println("Error al cargar menu.html: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Inicio Metodos de recuperacion de contraseña

        private void checkusername(String username) {
            System.out.println("JSBridge.checkUsername llamado con: " + username);

            try {
                String email = PasswordRecovery.getUserEmail(username);
                if (email != null) {
                    String maskedEmail = maskEmail(email);
                    System.out.println("usuario encontrado, email: " + maskedEmail);

                    engine.executeScript(
                            "document.getElementById('userFoundMessage').textContent = 'Usuario encontrado. Se enviará un código a: " + maskedEmail + "'; " +
                               "document.getElementById('userFoundMessage').style.display = 'block'; " +
                               "document.getElementById('securityQuestion').style.display = 'block'; " +
                               "document.getElementById('username').disabled = true;"
                    );
                } else {
                    System.out.println("Usuario no encontrado");
                    engine.executeScript(
                            "document.getElementById('errorMsg').textContent = 'Usuario no encontrado'; " +
                               "document.getElementById('errorMsg').style.display = 'block';"
                    );
                }
            } catch (SQLException e) {
                System.err.println("Error al verificar usuario: " + e.getMessage());
                engine.executeScript(
                        "document.getElementById('errorMsg').textContent = 'Error al verificar usuario: " + e.getMessage().replace("'", "\\'") + "'; " +
                           "document.getElementById('errorMsg').style.display = 'block';"
                );
            }
        }

        public void verifySecurityAnswer(String username, String securityAnswer) {
            System.out.println("JSBridege.verifySecurityAnswer llamado");

            try {
                boolean isValid = PasswordRecovery.verifySecurityQuestion(username, securityAnswer);
                if (isValid) {
                    String tempPassword = PasswordRecovery.resetPassword(username);
                    System.out.println("Respuesta correcta, contraseña reseteada");

                    engine.executeScript(
                            "document.getElementById('newPasswordMessage').textContent = 'Su nueva contraseña temporal es: " + tempPassword + "'; " +
                               "document.getElementById('newPasswordMessage').style.display = 'block'; " +
                               "document.getElementById('securityQuestion').style.display = 'none'; " +
                               "document.getElementById('resetComplete').style.display = 'block';"
                    );
                } else {
                    System.out.println("Respuesta de seguridad incorrecta");
                    engine.executeScript(
                            "document.getElementById('errorMsg').textContent = 'Respuesta de seguridad incorrecta'; " +
                               "document.getElementById('errorMsg').style.display = 'block';"
                    );
                }
            } catch (SQLException e) {
                System.err.println("Error al verificar respuesta: " + e.getMessage());
                engine.executeScript(
                        "document.getElementById('errorMsg').textContent = 'Error: " + e.getMessage().replace("'", "\\'") + "'; " +
                           "document.getElementById('errorMsg').style.display = 'block';"
                );
            }
        }

        private String maskEmail(String email) {
            if (email == null || email.isEmpty() || !email.contains("@")) {
                return "correo no disponible";
            }

            String [] parts = email.split("@");
            String name = parts[0];
            String domain = parts[1];

            String maskedName;
            if (name.length() > 4) {
                maskedName = name.substring(0,2) + "***" + name.substring(name.length() - 2);
            } else {
                maskedName = name.charAt(0) + "***";
            }

            return maskedName + "@" + domain;
        }


        public void requestPasswordReset(String email) {
            System.out.println("JSBridge.requestPasswordReset llamado con email: " + email);

            // Ejecutar en un hilo separado para no bloquear la UI
            new Thread(() -> {
                try {
                    // Primero verificamos si el email existe en nuestra base de datos
                    String username = getUsernameByEmail(email);
                    if (username == null) {
                        Platform.runLater(() -> {
                            try {
                                engine.executeScript(
                                        "document.getElementById('errorMsg').textContent = 'El correo electrónico no está registrado'; " +
                                                "document.getElementById('errorMsg').style.display = 'block';"
                                );
                            } catch (Exception ex) {
                                System.err.println("Error al mostrar mensaje: " + ex.getMessage());
                            }
                        });
                        return;
                    }

                    // Si el email existe, mostramos el formulario de pregunta de seguridad
                    Platform.runLater(() -> {
                        try {
                            String maskedEmail = maskEmail(email);
                            engine.executeScript(
                                    "document.getElementById('userFoundMessage').textContent = 'Usuario encontrado. Por favor responda la pregunta de seguridad.'; " +
                                            "document.getElementById('userFoundMessage').style.display = 'block'; " +
                                            "document.getElementById('securityQuestion').style.display = 'block'; " +
                                            "document.getElementById('email').disabled = true; " +
                                            "document.getElementById('requestResetBtn').style.display = 'none';"
                            );
                        } catch (Exception ex) {
                            System.err.println("Error al mostrar mensaje: " + ex.getMessage());
                        }
                    });

                } catch (Exception e) {
                    System.err.println("Error en requestPasswordReset: " + e.getMessage());
                    Platform.runLater(() -> {
                        try {
                            engine.executeScript(
                                    "document.getElementById('errorMsg').textContent = 'Error: " +
                                            e.getMessage().replace("'", "\\'") + "'; " +
                                            "document.getElementById('errorMsg').style.display = 'block';"
                            );
                        } catch (Exception ex) {
                            System.err.println("Error al mostrar mensaje: " + ex.getMessage());
                        }
                    });
                }
            }).start();
        }

        private String getUsernameByEmail(String email) throws SQLException {
            String username = null;
            final String query = "SELECT T_RH_Usuario FROM T_RH_InicioS WHERE T_RH_Email = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setString(1, email);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        username = rs.getString("T_RH_Usuario");
                    }
                }
            }

            return username;
        }

        public void verifyAndResetPassword(String email, String securityAnswer) {
            System.out.println("JSBridge.verifyAndResetPassword llamado con email: " + email);

            // Ejecutar en un hilo separado para no bloquear la UI
            new Thread(() -> {
                try {
                    // Obtener el username asociado al email
                    String username = getUsernameByEmail(email);
                    if (username == null) {
                        throw new Exception("Usuario no encontrado");
                    }

                    // Verificar la respuesta de seguridad
                    boolean isValid = PasswordRecovery.verifySecurityQuestion(username, securityAnswer);

                    if (isValid) {
                        // Si la respuesta es correcta, generamos una nueva contraseña
                        String tempPassword = PasswordRecovery.resetPassword(username);

                        // Mostrar la nueva contraseña al usuario
                        Platform.runLater(() -> {
                            try {
                                engine.executeScript(
                                        "document.getElementById('securityQuestion').style.display = 'none'; " +
                                                "document.getElementById('newPasswordMessage').textContent = 'Su nueva contraseña temporal es: " +
                                                tempPassword + "'; " +
                                                "document.getElementById('newPasswordMessage').style.display = 'block'; " +
                                                "document.getElementById('resetComplete').style.display = 'block';"
                                );
                            } catch (Exception ex) {
                                System.err.println("Error al mostrar mensaje: " + ex.getMessage());
                            }
                        });

                        // También podríamos integrar el envío del correo aquí usando la API Flask
                        try {
                            String token = PasswordRecoveryApi.requestPasswordReset(email);
                            if (token != null) {
                                System.out.println("Token de recuperación generado y correo enviado");
                            }
                        } catch (Exception e) {
                            System.err.println("Error al solicitar token de recuperación: " + e.getMessage());
                            // No mostramos este error al usuario ya que ya generamos la contraseña
                        }

                    } else {
                        Platform.runLater(() -> {
                            try {
                                engine.executeScript(
                                        "document.getElementById('errorMsg').textContent = 'Respuesta de seguridad incorrecta'; " +
                                                "document.getElementById('errorMsg').style.display = 'block';"
                                );
                            } catch (Exception ex) {
                                System.err.println("Error al mostrar mensaje: " + ex.getMessage());
                            }
                        });
                    }

                } catch (Exception e) {
                    System.err.println("Error en verifyAndResetPassword: " + e.getMessage());
                    Platform.runLater(() -> {
                        try {
                            engine.executeScript(
                                    "document.getElementById('errorMsg').textContent = 'Error: " +
                                            e.getMessage().replace("'", "\\'") + "'; " +
                                            "document.getElementById('errorMsg').style.display = 'block';"
                            );
                        } catch (Exception ex) {
                            System.err.println("Error al mostrar mensaje: " + ex.getMessage());
                        }
                    });
                }
            }).start();
        }

        // Finalización métodos de recuperación

        // Método para cargar diferentes páginas desde el menú
        public void loadPage(String pageType) {
            try {
                System.out.println("JSBridge.loadPage llamado con: " + pageType);
                this.currentPage = pageType;
                System.out.println("currentPage establecida en: " + this.currentPage);

                // runLater para asegurar que la carga ocurra en el hilo de la UI
                Platform.runLater(() -> {
                    try {
                        String pagePath = getClass().getResource("/table_view.html").toExternalForm();
                        System.out.println("Intentando cargar: " + pagePath);
                        engine.load(pagePath);
                        System.out.println("table_view.html cargado.");
                    } catch (Exception e) {
                        System.err.println("Error al cargar table_view.html: " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.err.println("Error en JSBridge.loadPage: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Método para regresar al menú principal
        public void goBackToMenu() {
            System.out.println("JSBridge.goBackToMenu llamado");
            Platform.runLater(() -> {
                try {
                    loadMenuPage(engine);
                    System.out.println("Volviendo al menú principal");
                } catch (Exception e) {
                    System.err.println("Error al volver al menú: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }

        // Método para inicializar la página actual con datos
        public void initPage() {
            System.out.println("JSBridge.initPage llamado para: " + this.currentPage);

            // verificar si tenemos un tipo de página válido
            if (this.currentPage == null || this.currentPage.isEmpty()) {
                System.err.println("Error: currentPage es null o vacío");
                // Notificar al usuario en la interfaz
                Platform.runLater(() -> {
                    try {
                        engine.executeScript(
                                "document.getElementById('error-message').textContent = 'Error: No se pudo determinar el tipo de página a cargar'; " +
                                        "document.getElementById('error-message').style.display = 'block'; " +
                                        "document.getElementById('loading-indicator').style.display = 'none';"
                        );
                    } catch (Exception ex) {
                        System.err.println("Error al mostrar mensaje de error" + ex.getMessage());
                    }
                });
                return;
            }

            try {
                System.out.println("Intentando cargar datos para: " + this.currentPage);

                // Usar un switch más resistente a fallos
                final String pageType = this.currentPage.toLowerCase().trim();
                switch (pageType) {
                    case "habitaciones":
                        System.out.println("Cargando datos de habitaciones...");
                        loadHabitacionesData();
                        break;
                    case "huespedes":
                        System.out.println("Cargando datos de huéspedes...");
                        loadHuespedesData();
                        break;
                    case "pagos":
                        System.out.println("Cargando datos de pagos...");
                        loadPagosData();
                        break;
                    case "reserva-servicios":
                        System.out.println("Cargando datos de reserva de servicios...");
                        loadReservaServiciosData();
                        break;
                    case "reservas":
                        System.out.println("Cargando datos de reservas...");
                        loadReservasData();
                        break;
                    case "servicios":
                        System.out.println("Cargando datos de servicios...");
                        loadServiciosData();
                        break;
                    default:
                        System.out.println("Tipo de página no reconocido: " + this.currentPage);
                        showErrorInUI("Tipo de página no reconocido: " + this.currentPage);
                        return;
                }
            } catch (Exception e) {
                System.err.println("Error al inicializar página: " + e.getMessage());
                e.printStackTrace();
                // Mostrar el error en la interfaz web
                showErrorInUI("Error al cargar datos: " + e.getMessage());
            }

            System.out.println("JSBridge.initPage completado para: " + this.currentPage);
        }

        // Método auxiliar para mostrar errores en la interfaz de usuario
        private void showErrorInUI(String errorMessage) {
            if (errorMessage == null) {
                errorMessage = "Error desconocido";
            }

            // Escapar comillas simples para evitar errores en JavaScript
            final String safeErrorMessage = errorMessage.replace("'", "\\'");

            // Ejecutar en el hilo de JavaFX
            Platform.runLater(() -> {
                try {
                    engine.executeScript(
                            "document.getElementById('error-message').textContent = '" + safeErrorMessage + "'; " +
                                    "document.getElementById('error-message').style.display = 'block'; " +
                                    "document.getElementById('loading-indicator').style.display = 'none';"
                    );
                } catch (Exception ex) {
                    System.err.println("Error al mostrar mensaje de error en la UI: " + ex.getMessage());
                }
            });
        }

        // Métodos para cargar datos específicos
        private void loadHabitacionesData() {
            loadTableData("T_RH_Habitaciones", "Habitaciones", null);
        }

        private void loadHuespedesData() {
            loadTableData("T_RH_Huespedes", "Huéspedes", null);
        }

        private void loadPagosData() {
            loadTableData("T_RH_Pagos", "Pagos", null);
        }

        private void loadReservaServiciosData() {
            loadTableData("T_RH_Reserva_Servicios", "Reservas de Servicios", null);
        }

        private void loadReservasData() {
            loadTableData("T_RH_Reservas", "Reservas", null);
        }

        private void loadServiciosData() {
            loadTableData("T_RH_Servicios", "Servicios", null);
        }

        // Método genérico para cargar datos de cualquier tabla
        private void loadTableData(String tableName, String pageTitle, String whereClause) {
            final String query = "SELECT * FROM " + tableName +
                    (whereClause != null && !whereClause.isEmpty() ? " WHERE " + whereClause : "");

            System.out.println("Ejecutando consulta: " + query);

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {

                System.out.println("Consulta ejecutada correctamente");

                final ResultSetMetaData metaData = rs.getMetaData();
                final int columnCount = metaData.getColumnCount();
                System.out.println("Número de columnas encontradas: " + columnCount);

                // Lista para almacenar nombres de columnas
                final List<String> columns = new ArrayList<>();
                final List<String> originalColumns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    originalColumns.add(metaData.getColumnName(i));
                    columns.add(metaData.getColumnName(i).toLowerCase());
                    System.out.println("Columna " + i + ": " + metaData.getColumnName(i) +
                            ", Tipo: " + metaData.getColumnTypeName(i) +
                            ", Clase: " + metaData.getColumnClassName(i));
                }
                System.out.println("Columnas: " + columns);

                // Crear JSONArray para los datos
                final JSONArray jsonData = new JSONArray();
                int rowCount = 0;

                // Verificar si hay datos
                if (!rs.isBeforeFirst()) {
                    System.out.println("La consulta no devolvió ningún dato");
                    Platform.runLater(() -> {
                        try {
                            engine.executeScript(
                                    "document.getElementById('error-message').textContent = 'No se encontraron datos en la tabla'; " +
                                            "document.getElementById('error-message').style.display = 'block'; " +
                                            "document.getElementById('loading-indicator').style.display = 'none';"
                            );
                        } catch (Exception ex) {
                            System.err.println("Error al mostrar mensaje de 'sin datos': " + ex.getMessage());
                        }
                    });
                    return;
                }

                // Procesar filas
                while (rs.next()) {
                    rowCount++;
                    final JSONObject row = new JSONObject();
                    for (int i = 0; i < columns.size(); i++) { // Iterar por índices
                        try {
                            Object value = rs.getObject(originalColumns.get(i)); // Usar nombre original
                            row.put(columns.get(i), value != null ? value.toString() : ""); // Usar nombre en minúsculas
                        } catch (Exception e) {
                            System.err.println("Error al procesar valor para columna " + columns.get(i) + ": " + e.getMessage());
                            row.put(columns.get(i), "");
                        }
                    }
                    jsonData.add(row);
                }
                System.out.println("Filas procesadas: " + rowCount);

                for (int i = 0; i < jsonData.size(); i++) {
                    System.out.println("Verificando fila " + i);
                    JSONObject row = (JSONObject) jsonData.get(i);
                    for (Object key : row.keySet()) {
                        Object value = row.get(key);
                        if (value != null && value.toString().contains("\u0000")) {
                            System.out.println("ADVERTENCIA: Valor con caracteres nulos detectado en " + key);
                            row.put(key, value.toString().replace("\u0000", ""));
                        }
                    }
                }

                // Crear JSONArray para columnas
                final JSONArray jsonColumns = new JSONArray();
                jsonColumns.addAll(columns);

                // Serialización segura para verificar
                final String columnsJsonString = jsonColumns.toJSONString();
                final String dataJsonString = jsonData.toJSONString();

                // En Main.java, puedes agregar una validación del tamaño
                System.out.println("Tamaño aproximado de los datos en bytes: " + dataJsonString.getBytes().length);
                if (dataJsonString.getBytes().length > 1000000) {
                    System.out.println("ADVERTENCIA: Los datos son muy grandes, esto podría causar problemas");
                }

                System.out.println("Columns JSON: " + columnsJsonString);
                System.out.println("Data JSON (primeras filas): " +
                        (jsonData.size() > 0 ? ((JSONObject) jsonData.get(0)).toJSONString() : "Sin datos"));

                // Script seguro para envío a JavaScript
                final String safePageTitle = pageTitle.replace("'", "\\'").replace("\n", " ");

                final String script =
                        "try {\n" +
                                "  console.log('Llamando a displayData...');\n" +
                                "  if (typeof displayData === 'function') {\n" +
                                "    displayData('" + safePageTitle + "', " + columnsJsonString + ", " + dataJsonString + ");\n" +
                                "    console.log('displayData ejecutado correctamente');\n" +
                                "  } else {\n" +
                                "    console.error('Error: displayData no es una función');\n" +
                                "    document.getElementById('error-message').textContent = 'Error: Función displayData no encontrada';\n" +
                                "    document.getElementById('error-message').style.display = 'block';\n" +
                                "    document.getElementById('loading-indicator').style.display = 'none';\n" +
                                "  }\n" +
                                "} catch(e) {\n" +
                                "  console.error('Error en displayData: ' + e);\n" +
                                "  document.getElementById('error-message').textContent = 'Error al mostrar datos: ' + e.message;\n" +
                                "  document.getElementById('error-message').style.display = 'block';\n" +
                                "  document.getElementById('loading-indicator').style.display = 'none';\n" +
                                "}";

                // En la clase Main.java, en el método loadTableData, añade más logs para depurar
                System.out.println("Data JSON completo: " + dataJsonString);
                System.out.println("Longitud de dataJsonString: " + dataJsonString.length());

                System.out.println("Enviando datos a JavaScript: " + jsonData.size() + " filas");

                // Asegurarse de ejecutar en el hilo de JavaFX
                Platform.runLater(() -> {
                    try {
                        engine.executeScript(script);
                        System.out.println("Script enviado correctamente");
                    } catch (Exception e) {
                        System.err.println("Error al ejecutar script JavaScript: " + e.getMessage());
                        e.printStackTrace();

                        try {
                            engine.executeScript(
                                    "document.getElementById('error-message').textContent = 'Error al mostrar datos: " +
                                            e.getMessage().replace("'", "\\'") + "';\n" +
                                            "document.getElementById('error-message').style.display = 'block';\n" +
                                            "document.getElementById('loading-indicator').style.display = 'none';"
                            );
                        } catch (Exception ex) {
                            System.err.println("Error secundario al mostrar mensaje de error: " + ex.getMessage());
                        }
                    }
                });

            } catch (SQLException e) {
                System.err.println("Error SQL al ejecutar consulta: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    try {
                        engine.executeScript(
                                "document.getElementById('error-message').textContent = 'Error de base de datos: " +
                                        e.getMessage().replace("'", "\\'") + "';\n" +
                                        "document.getElementById('error-message').style.display = 'block';\n" +
                                        "document.getElementById('loading-indicator').style.display = 'none';"
                        );
                    } catch (Exception ex) {
                        System.err.println("Error al mostrar mensaje de error SQL: " + ex.getMessage());
                    }
                });
            } catch (Exception e) {
                System.err.println("Error inesperado en loadTableData: " + e.getMessage());
                e.printStackTrace();

                Platform.runLater(() -> {
                    try {
                        engine.executeScript(
                                "document.getElementById('error-message').textContent = 'Error inesperado: " +
                                        e.getMessage().replace("'", "\\'") + "';\n" +
                                        "document.getElementById('error-message').style.display = 'block';\n" +
                                        "document.getElementById('loading-indicator').style.display = 'none';"
                        );
                    } catch (Exception ex) {
                        System.err.println("Error al mostrar mensaje de error inesperado: " + ex.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public void start(Stage stage) {
        try {
            WebView webView = new WebView();
            WebEngine engine = webView.getEngine();

            // Habilitar la consola JavaScript
            webView.getEngine().setJavaScriptEnabled(true);

            // Configurar la redirección de la consola
            JSObject window = (JSObject) engine.executeScript("window");
            window.setMember("javaConsole", System.out);
            engine.executeScript(
                    "console.log = function(message) { javaConsole.println('JS LOG: ' + message); };" +
                            "console.error = function(message) { javaConsole.println('JS ERROR: ' + message); };"
            );

            // Crear una única instancia de JSBridge que se mantendrá durante toda la aplicación
            bridge = new JSBridge(engine);

            // Listener para el estado de carga, usando la misma instancia de JSBridge
            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                System.out.println("Estado de carga cambiado: " + oldState + " -> " + newState);

                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    System.out.println("Página cargada exitosamente: " + engine.getLocation());

                    // Usar Platform.runLater para retrasar la asignación de JSBridge
                    Platform.runLater(() -> {
                        try {
                            System.out.println("Obteniendo objeto window...");
                            JSObject win = (JSObject) engine.executeScript("window");

                            System.out.println("Asignando JSBridge existente a window.app...");
                            win.setMember("app", bridge);

                            System.out.println("JSBridge asignado para: " + engine.getLocation());

                            // Verificar si app está disponible desde JavaScript
                            engine.executeScript(
                                    "if (typeof app !== 'undefined') {" +
                                            "  console.log('app está definido correctamente en JavaScript');" +
                                            "} else {" +
                                            "  console.error('app NO está definido en JavaScript');" +
                                            "}"
                            );

                            // Habilitar los botones si estamos en la página del menú
                            if (engine.getLocation().contains("menu.html")) {
                                System.out.println("Habilitando botones del menú...");
                                engine.executeScript(
                                        "document.querySelectorAll('.menu-button').forEach(btn => { " +
                                                "  btn.disabled = false; " +
                                                "  console.log('Botón habilitado: ' + btn.id); " +
                                                "});"
                                );
                            }
                        } catch (Exception e) {
                            System.err.println("Error en Platform.runLater al asignar JSBridge: " + e.getMessage());
                            e.printStackTrace();
                        }
                    });

                } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                    System.err.println("Error al cargar la página: " + engine.getLoadWorker().getException());
                    if (engine.getLoadWorker().getException() != null) {
                        engine.getLoadWorker().getException().printStackTrace();
                    }
                }
            });

            // Configurar manejo de excepciones no capturadas
            Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
                System.err.println("Excepción no capturada en hilo " + thread.getName() + ": " + throwable.getMessage());
                throwable.printStackTrace();
            });

            // Cargar la página inicial (index.html)
            System.out.println("Cargando index.html...");
            engine.load(getClass().getResource("/index.html").toExternalForm());

            stage.setTitle("Sistema Hotel");
            stage.setScene(new Scene(webView, 900, 600));
            stage.show();

        } catch (Exception e) {
            System.err.println("Error en start: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        System.out.println("Aplicación cerrándose, cerrando conexión a la base de datos...");
        DatabaseConnection.closeConnection();
        System.out.println("Aplicación cerrada correctamente");
    }
}