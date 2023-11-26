package org.apache.dubbo.testing;

import cn.hutool.http.HttpUtil;
import com.sun.net.httpserver.HttpServer;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;


public class WebHookServer {


    public static void main(String[] args) {
        String owner = "wxbty";
        String repo = "jmh_demo";
        String path = "pom.xml";

        String latestCommitId = getLatestCommitId(owner, repo);
        System.out.println("Latest Commit ID: " + latestCommitId);

        String version = getPomVersion(owner, repo, path);
        System.out.println("POM Version: " + version);
    }

    public static String getLatestCommitId(String owner, String repo) {


        String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/commits/master";


        try {
            URL url = new URL(apiUrl);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            if (connection.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString().split("\"sha\":\"")[1].split("\"")[0];
            } else {
                throw new RuntimeException("Failed to fetch data from GitHub API: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void main1(String[] args) throws Exception {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(8081), 0);

        httpServer.createContext("/metrics/notify", httpExchange -> {

            String owner = "wxbty";
            String repo = "jmh_demo";
            String path = "pom.xml";
            String latestCommitId = getLatestCommitId(owner, repo);
            String version = getPomVersion(owner, repo, path);


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
        Connection conn = DriverManager.getConnection(URL, USR, PSD);
        Statement statement = conn.createStatement();
        statement.execute("drop table if exists db_user");
        statement.execute("create table db_user ( id NUMBER, name VARCHAR2(10) )");
        statement.execute("insert into db_user(id,name) values (1,'hhh')");

        ResultSet resultSet = statement.executeQuery("select * from db_user");

        while (resultSet.next()) {
            System.out.println(resultSet.getInt("id"));
            System.out.println(resultSet.getString("name"));
        }

        httpServer.start();

    }


    public static String getPomVersion(String owner, String repo, String path) {
        String apiUrl = "https://raw.githubusercontent.com/" + owner + "/" + repo + "/master/" + path;
        String text = HttpUtil.get(apiUrl);
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;
        try {
            model = reader.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        }
        return model.getVersion(); // Assuming version is in the <version> tag of the pom.xml
    }


}
