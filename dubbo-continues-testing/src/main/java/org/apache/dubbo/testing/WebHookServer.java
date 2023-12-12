package org.apache.dubbo.testing;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


public class WebHookServer {

    static String URL = "jdbc:h2:file:~/h2/test";
    static String USR = "sa";
    static String PSD = "666666";
    static String owner = "wxbty";
    static String repo = "dubbo";

    static String branch = "ct_test";

    public static void main(String[] args) {
        try {
            Class.forName("org.h2.Driver");
            Connection conn = DriverManager.getConnection(URL, USR, PSD);
            Statement statement = conn.createStatement();
            statement.execute("drop table if exists db_commit_info");
            statement.execute("create table db_commit_info ( id INT NOT NULL AUTO_INCREMENT, commit_id VARCHAR(64) , content text )");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        HttpServer httpServer;
        try {
            httpServer = HttpServer.create(new InetSocketAddress(8081), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        httpServer.createContext("/merge/notify", httpExchange -> {

            String apiUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/commits/" + branch;
            System.out.println(apiUrl);
            String latestCommitId = httpExchange.getRequestHeaders().getFirst("latestCommitId");
            if (latestCommitId == null) {
                latestCommitId = getLatestCommitId(apiUrl);
            }
            String pomUrl = "https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + branch + "/pom.xml";
            System.out.println(pomUrl);
            String version = httpExchange.getRequestHeaders().getFirst("version");
            if (version == null) {
                version = getPomVersion(pomUrl);
            }
            // exec installNewVersion.sh under resources in the current project root directory
            URL resource = WebHookServer.class.getClassLoader().getResource("installNewest.sh");
            String userDir = System.getProperty("user.dir");

            if (resource != null) {
                String scriptPath = resource.getPath();
                System.out.println("Script Path: " + scriptPath);

                System.out.println("User Dir: " + userDir);
                ProcessBuilder processBuilder = new ProcessBuilder(scriptPath, userDir, version);
                Process process;
                try {
                    process = processBuilder.start();
                } catch (IOException e) {
                    System.out.println("Failed to execute shell script: " + e.getMessage());
                    throw new RuntimeException(e);
                }

//                BufferedInputStream errIn = new BufferedInputStream(process.getErrorStream());
//                BufferedReader errBr = new BufferedReader(new InputStreamReader(errIn));
                String lineStr;
//                while ((lineStr = errBr.readLine()) != null) {
//                    System.out.println("error:" + lineStr);
//                }

//                StringBuilder result = new StringBuilder();
                BufferedInputStream in = null;
                BufferedReader br = null;

                try {
                    // 获取shell返回流
                    in = new BufferedInputStream(process.getInputStream());
                    // 字符流转换字节流
                    br = new BufferedReader(new InputStreamReader(in));
                    // 这里也可以输出文本日志
                    System.out.println("============result begin============");
                    while (process.isAlive() && (lineStr = br.readLine()) != null) {
//                        result.append(lineStr);
                        System.out.println(lineStr);
//                        result.append("\n");
                    }
//                    System.out.println(result);
                    System.out.println("============result end============");
                } finally {
                    // 关闭输入流
                    br.close();
                    in.close();
                }


                try {
                    process.waitFor();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            System.out.println("process end");

            File outputFile = new File(userDir + "/dubbo-continues-testing-demo/data/output.json");
            // transfer file to Json
            if (!outputFile.exists()) {
                throw new RuntimeException("output file not exists");
            }
            String json;
            try {
                json = FileUtils.readFileToString(outputFile, StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            json = json.replace("currentCommitId", latestCommitId);

            try {
                Class.forName("org.h2.Driver");
                Connection conn = DriverManager.getConnection(URL, USR, PSD);
                Statement statement = conn.createStatement();
                statement.execute("insert into db_commit_info(commit_id,content) values ('" + latestCommitId + "','" + json + "')");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            mergeJson();

            byte[] respContents = "ok".getBytes(StandardCharsets.UTF_8);
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(200, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            httpExchange.close();
        });

        httpServer.createContext("/merge/query", httpExchange -> {

            try {
                getJsonObjects();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            byte[] respContents = "ok".getBytes(StandardCharsets.UTF_8);
            httpExchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            httpExchange.sendResponseHeaders(200, respContents.length);
            httpExchange.getResponseBody().write(respContents);
            httpExchange.close();
        });

        httpServer.start();

        System.out.println("WebHookServer started");
    }

    public static String getLatestCommitId(String apiUrl) {

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
                System.out.println(apiUrl);
                System.out.println("Failed to fetch data from GitHub API: " + connection.getResponseCode());
                throw new RuntimeException("Failed to fetch data from GitHub API: " + connection.getResponseCode());
            }
        } catch (IOException e) {
            System.out.println("Failed to fetch data from GitHub API: " + e.getMessage());
            throw new RuntimeException(e);
        }

    }

    public static String getPomVersion(String pomUrl) {

        String text = HttpUtil.get(pomUrl);
        MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model;
        try {
            model = reader.read(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return model.getVersion(); // Assuming version is in the <version> tag of the pom.xml
    }

    public static void mergeJson() {

        try {
            List<JSONObject> list = getJsonObjects();
            JSONArray array = JSONUtil.parseArray(list);
            String userDir = System.getProperty("user.dir");
            System.out.println("merge userDir: " + userDir);
            FileUtils.writeStringToFile(new File(userDir + "/data/output_merge.json"), array.toStringPretty(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private static List<JSONObject> getJsonObjects() throws ClassNotFoundException, SQLException {
        Class.forName("org.h2.Driver");
        Connection conn = DriverManager.getConnection(URL, USR, PSD);
        Statement statement = conn.createStatement();
        ResultSet resultSet = statement.executeQuery("select * from db_commit_info");
        List<JSONObject> list = new ArrayList<>();
        while (resultSet.next()) {
            String commit_id = resultSet.getString("commit_id");
            System.out.println(commit_id);
            String commitContent = resultSet.getString("content");
            System.out.println(commitContent);
            JSONArray array = JSONUtil.parseArray(commitContent);
            list.addAll(array.toList(JSONObject.class));
        }
        return list;
    }


}
