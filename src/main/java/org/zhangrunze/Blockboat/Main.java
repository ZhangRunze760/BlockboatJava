package org.zhangrunze.Blockboat;

import com.sun.net.httpserver.HttpServer;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
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
    private static final String exanpleConfig = """
            QQ :
              QQGroup_id : "123456" # 您的QQ群号。不影响openai的全面使用（若开启）。
              QQAPI : "http://127.0.0.1:5700/" # 在此输入您已经配置好的go-cqhttp接口。
              QQPort : 5710 # go-cqhttp的http post上报器端口。
              OPENAI_APIKEY : "*****************************************" # openai的apikey。填写非法时禁用openai。

            MC :
              MCURL : "http://127.0.0.1:23333" # 在此输入您的MCSManager链接。开启RCON时为RCON地址（如http://127.0.0.1:25575）。
              MCUUID : "**************************" #在此输入您在MCSManager中的实例的UID。开启RCON后无效。
              MCREMOTE_UUID : "**************************" # 在此输入您在MCSManager中的实例所对应的节点的ID，也是实例的GID。同上。
              MCAPIKEY : "****************************" # 在此输入您在MCSManager中配置好的APIKEY。同上。
              MCLOG : "./logs/latest.log" # 在此输入您的MC服务端的latest.log的位置（绝对位置与相对位置均可）。
              JavaEdition: "true" # 在此填入您的MC服务端对应的版本，如果是java版则填true，如果是基岩版则填false。
              enabledRcon : "true" # 是否开启RCON模式。RCON模式在Windows系统上的兼容性相对较强，如果出现乱码，建议开启；
                                   # 否则请避免使用RCON模式，以免服务器出现安全隐患。
              rconPort : 25575 # RCON端口。开启RCON后有效。
              rconPassword : "*************************" # RCON密码。开启RCON后有效。
            """;
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
    public static void main(String[] args) throws IOException {

        System.out.println("机器人正在开启。。。");

        Yaml yaml = new Yaml();
        InputStream in;
        {
            try {
                in = new FileInputStream("botconfig.yaml");
            } catch (FileNotFoundException e) {
                System.out.println("未检测到配置文件，在本目录下生成botconfig.yaml。");
                Files.writeString(Paths.get("botconfig.yaml"), exanpleConfig);
                System.out.println("请填写配置文件后重启机器人。");
                return;
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
        new SendMessage(qqapi_url, mcapi_url, mcapi_uid, mcapi_gid, mcapikey, enabledRcon, rconPort, rconPassword);

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

        new RequestProcessing(qqgroup_id, qqapi_url, mcapi_url, mcapi_uid, mcapi_gid, mcapikey, OPENAI_API, isJavaEdition, enabledRcon, rconPort, rconPassword);
        ListenOnAPort(qqport);
    }
}