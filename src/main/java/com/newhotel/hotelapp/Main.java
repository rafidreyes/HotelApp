package com.newhotel.hotelapp;

import javafx.application.Application;
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

    public static void main(String[] args) {
        launch(args);
    }

    public class JSBridge {
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
                // Manejar el error (por ejemplo, mostrar un mensaje en la consola JavaScript)
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

        // Método para cargar diferentes páginas desde el menú
        public void loadPage(String pageType) {
            try {
                System.out.println("JSBridge.loadPage llamado con: " + pageType);
                this.currentPage = pageType;
                System.out.println("currentPage establecida en: " + this.currentPage);
                String pagePath = getClass().getResource("/table_view.html").toExternalForm();
                engine.load(pagePath);
                System.out.println("table_view.html cargado."); // Agregar este log
            } catch (Exception e) {
                System.out.println("Error en JSBridge.loadPage: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // Método para regresar al menú principal
        public void goBackToMenu() {
            loadMenuPage(engine);
        }

        // Método para inicializar la página actual con datos
        public void initPage() {
            System.out.println("JSBridge.initPage llamado para: " + this.currentPage);
            try {
                switch (this.currentPage) {
                    case "habitaciones":
                        loadHabitacionesData();
                        break;
                    case "huespedes":
                        loadHuespedesData();
                        break;
                    case "pagos":
                        loadPagosData();
                        break;
                    case "reserva-servicios":
                        loadReservaServiciosData();
                        break;
                    case "reservas":
                        loadReservasData();
                        break;
                    case "servicios":
                        loadServiciosData();
                        break;
                    default:
                        System.out.println("Tipo de página no reconocido: " + this.currentPage);
                }
            } catch (Exception e) {
                System.out.println("Error al inicializar página: " + e.getMessage());
                e.printStackTrace();
            }
            System.out.println("JSBridge.initPage completado para: " + this.currentPage);
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

            try (Connection conn = DatabaseConnection.getConnection(); // Obtener la conexión
                 PreparedStatement stmt = conn.prepareStatement(query); // Preparar la consulta
                 ResultSet rs = stmt.executeQuery()) { // Ejecutar la consulta

                final ResultSetMetaData metaData = rs.getMetaData();
                final int columnCount = metaData.getColumnCount();

                final List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columns.add(metaData.getColumnName(i));
                }

                final JSONArray jsonData = new JSONArray();
                while (rs.next()) {
                    final JSONObject row = new JSONObject();
                    for (String column : columns) {
                        Object value = rs.getObject(column);
                        row.put(column, value != null ? value.toString() : "");
                    }
                    jsonData.add(row);
                }

                final JSONArray jsonColumns = new JSONArray();
                jsonColumns.addAll(columns);

                final String script = "displayData(" +
                        "'" + pageTitle.replace("'", "\\'") + "', " +
                        jsonColumns.toJSONString() + ", " +
                        jsonData.toJSONString() + ")";

                System.out.println("Enviando datos a JavaScript: " + jsonData.size() + " filas");
                engine.executeScript(script);

            } catch (SQLException e) {
                System.err.println("Error al ejecutar consulta: " + e.getMessage());
                e.printStackTrace();
                // Manejar el error (por ejemplo, mostrar un mensaje en la consola JavaScript)
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

            // Usamos un array de un solo elemento para poder modificar la referencia dentro del lambda
            final JSObject[] windowRef = new JSObject[1];
            windowRef[0] = (JSObject) engine.executeScript("window");
            windowRef[0].setMember("javaConsole", System.out);
            engine.executeScript(
                    "console.log = function(message) { javaConsole.println(message); };" +
                            "console.error = function(message) { javaConsole.println('ERROR: ' + message); };"
            );

            System.out.println("Cargando index.html...");
            engine.load(getClass().getResource("/index.html").toExternalForm());

            // Inicializar JSBridge *una sola vez* después de cargar index.html
            engine.getLoadWorker().stateProperty().addListener((obs, old, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    System.out.println("index.html cargado correctamente");
                    try {
                        windowRef[0] = (JSObject) engine.executeScript("window");
                        JSBridge bridge = new JSBridge(engine); // Crear una instancia de JSBridge
                        windowRef[0].setMember("app", bridge); // Exponer el objeto 'app' a JavaScript
                        System.out.println("JSBridge configurado en window.app");
                    } catch (Exception e) {
                        System.out.println("Error al configurar JSBridge: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (newState == javafx.concurrent.Worker.State.FAILED) {
                    System.out.println("Error al cargar index.html: " +
                            engine.getLoadWorker().getException());
                }
            });


            stage.setTitle("Sistema Hotel");
            stage.setScene(new Scene(webView, 900, 600));
            stage.show();
        } catch (Exception e) {
            System.out.println("Error en start: " + e.getMessage());
            e.printStackTrace();
        }
    }
}