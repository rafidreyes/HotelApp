module com.newhotel.hotelapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;
    requires javafx.media;
    requires jdk.jsobject;
    requires java.sql;
    requires json.simple;
    requires okhttp3;


    opens com.newhotel.hotelapp to javafx.fxml;
    exports com.newhotel.hotelapp;
}