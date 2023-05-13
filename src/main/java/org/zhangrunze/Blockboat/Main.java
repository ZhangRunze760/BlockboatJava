package org.zhangrunze.Blockboat;

import com.sun.net.httpserver.HttpServer;
import org.yaml.snakeyaml.*;

import java.io.*;
import java.net.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;

class Main {
    static String qqapi_url;
    static String qqgroup_id;
    static int qqport;
    static String mcapi_url;
    static String mcapi_uid;
    static String mcapi_gid;
    static String mcapikey;
    static String OPENAI_API;
    static boolean isJavaEdition;
    static String mclog;
    static boolean enabledRcon;
    static int rconPort;
    static String rconPassword;
    private static String lastLine = "";
    private static String currentLine = "";

    public static void ListenOnAPort(int port) {
        System.out.printf("机器人正在监听%d端口。请确保GO-CQHTTP正在该端口上运行。\n", port);
        try {
            HttpServer httpServer = HttpServer.create(new InetSocketAddress(port), 0);
            httpServer.createContext("/", new MyHandler());
            httpServer.setExecutor(Executors.newCachedThreadPool());
            httpServer.start();

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    public static void main(String[] args) {

        System.out.println("机器人正在开启。。。");

        Yaml yaml = new Yaml();
        InputStream in;
        {
            try {
                in = new FileInputStream("botconfig.yaml");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        Map<String, Object> obj = yaml.load(in);

        try {
            qqapi_url = ((LinkedHashMap<String, String>) obj.get("QQ")).get("QQAPI");
            qqgroup_id = ((LinkedHashMap<String, String>) obj.get("QQ")).get("QQGroup_id");
            qqport = ((LinkedHashMap<String, Integer>) obj.get("QQ")).get("QQPort");
            OPENAI_API = ((LinkedHashMap<String, String>) obj.get("QQ")).get("OPENAI_APIKEY");

            mcapi_url = ((LinkedHashMap<String, String>) obj.get("MC")).get("MCURL");
            mcapi_uid = ((LinkedHashMap<String, String>) obj.get("MC")).get("MCUUID");
            mcapi_gid = ((LinkedHashMap<String, String>) obj.get("MC")).get("MCREMOTE_UUID");
            mcapikey = ((LinkedHashMap<String, String>) obj.get("MC")).get("MCAPIKEY");
            mclog = ((LinkedHashMap<String, String>) obj.get("MC")).get("MCLOG");
            isJavaEdition = Boolean.parseBoolean(((LinkedHashMap<String, String>) obj.get("MC")).get("JavaEdition"));
            enabledRcon = Boolean.parseBoolean(((LinkedHashMap<String, String>) obj.get("MC")).get("enabledRcon"));
            rconPort = ((LinkedHashMap<String, Integer>) obj.get("MC")).get("rconPort");
            rconPassword = ((LinkedHashMap<String, String>) obj.get("MC")).get("rconPassword");
        } catch (NullPointerException e) {
            System.out.println("配置文件格式有误！");
            return;
        }
        MCMessageProcessing mcMessageProcessing = new MCMessageProcessing(isJavaEdition, qqapi_url, mcapi_url, mcapi_uid, mcapi_gid, mcapikey, qqgroup_id, enabledRcon, rconPort, rconPassword);
        SendMessage sendMessage = new SendMessage(qqapi_url, mcapi_url, mcapi_uid, mcapi_gid, mcapikey, enabledRcon, rconPort, rconPassword);

        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new FileReader(mclog))) {
                while (true) {
                    // 每隔1秒读取一次文件
                    currentLine = reader.readLine();
                    if (currentLine != null && !currentLine.equals(lastLine)) {
                        // 如果当前行不为空且与上一行不同，就存储并处理该行
                        lastLine = currentLine;
                        String Message = currentLine;
                        mcMessageProcessing.Process(Message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("正在开启消息处理模块。。。");

        RequestProcessing requestProcessing = new RequestProcessing(qqgroup_id, qqapi_url, mcapi_url, mcapi_uid, mcapi_gid, mcapikey, OPENAI_API, isJavaEdition, enabledRcon, rconPort, rconPassword);
        ListenOnAPort(qqport);
    }
}