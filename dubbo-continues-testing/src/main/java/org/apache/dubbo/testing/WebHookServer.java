package org.apache.dubbo.testing;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class WebHookServer {

    public static void main(String[] args) throws IOException, ClassNotFoundException, SQLException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8081), 0);

        httpServer.createContext("/metrics/notify", httpExchange -> {
            byte[] respContents = "ok".getBytes(StandardCharsets.UTF_8);
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(200, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            httpExchange.close();
        });

        String URL = "jdbc:h2:/Users/zcy/data/h2Data";
        String USR = "sa";
        String PSD = "666666";
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection(URL,USR,PSD);
        Statement statement = conn.createStatement();
        statement.execute("drop table if exists db_user");
        statement.execute("create table db_user ( id NUMBER, name VARCHAR2(10) )");
        statement.execute("insert into db_user(id,name) values (1,'hhh')");

        ResultSet resultSet = statement.executeQuery("select * from db_user");

        while(resultSet.next()){
            System.out.println(resultSet.getInt("id"));
            System.out.println(resultSet.getString("name"));
        }

        httpServer.start();

    }
}
