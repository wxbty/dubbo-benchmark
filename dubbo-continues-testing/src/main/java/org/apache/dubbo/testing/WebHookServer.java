package org.apache.dubbo.testing;

import cn.hutool.core.io.IoUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.io.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class WebHookServer {

    static String URL = "jdbc:h2:file:~/h2/test";
    static String USR = "sa";
    static String PSD = "666666";

    static String currentVersion = "3.3.0-beta.2-SNAPSHOT";

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

        httpServer.createContext("/dubbo/notify", httpExchange -> {

            InputStream stream = httpExchange.getRequestBody();
            String body = IoUtil.read(stream, StandardCharsets.UTF_8);
            JSONObject jsonObject = JSONUtil.parseObj(body);
            String latestCommitId = jsonObject.getStr("after");
            String xmlUrl = "https://gitee.com/apache/dubbo/raw/3.3/pom.xml";
            String version = getCurrentVersion(xmlUrl);
            if (version == null) {
                version = currentVersion;
            }

            URL resource = WebHookServer.class.getClassLoader().getResource("installNewest.sh");
            String userDir = System.getProperty("user.dir");

            if (resource != null) {
                String scriptPath = resource.getPath();
                System.out.println("Script Path: " + scriptPath);

                System.out.println("User Dir: " + userDir);
                String username = System.getProperty("user.name");
                ProcessBuilder processBuilder = new ProcessBuilder("sudo", "-u", username, "sh", scriptPath, userDir, version);
                Process process;
                try {
                    process = processBuilder.start();
                } catch (IOException e) {
                    System.out.println("Failed to execute shell script: " + e.getMessage());
                    throw new RuntimeException(e);
                }

                String lineStr;
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
                        System.out.println(lineStr);
                    }
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

    public static String getCurrentVersion(String xmlUrl) {


        // 下载XML文件
        try {
            // 获取 HTML 内容
            URL url = new URL(xmlUrl);
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            reader.close();
            String htmlContent = stringBuilder.toString();

            // 使用正则表达式提取 <revision> 标签中的内容
            Pattern pattern = Pattern.compile("<revision>(.*?)</revision>");
            Matcher matcher = pattern.matcher(htmlContent);

            // 遍历匹配结果并输出版本号
            while (matcher.find()) {
                String revisionValue = matcher.group(1);
                return revisionValue;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


}
