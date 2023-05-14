package org.zhangrunze.Blockboat;

import com.alibaba.fastjson2.JSONObject;
import net.kronos.rkon.core.Rcon;
import net.kronos.rkon.core.ex.AuthenticationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SendMessage {

    static String qqapi_url;
    static String mcapi_url;
    static String mcapi_uid;
    static String mcapi_gid;
    static String mcapikey;
    static boolean enabledRcon;
    static int rconPort;
    static String rconPassword;

    SendMessage(String qqapi_url, String mcapi_url, String mcapi_uid, String mcapi_gid, String mcapikey, boolean enabledRcon, int rconPort, String rconPassword) {
        SendMessage.qqapi_url = qqapi_url;
        SendMessage.mcapi_url = mcapi_url;
        SendMessage.mcapi_uid = mcapi_uid;
        SendMessage.mcapi_gid = mcapi_gid;
        SendMessage.mcapikey = mcapikey;
        SendMessage.enabledRcon = enabledRcon;
        SendMessage.rconPassword = rconPassword;
        SendMessage.rconPort = rconPort;
    }
    public static void SendGroupMessage(long group_id, long user_id, String Message) {
        String GETBody = qqapi_url + "send_group_msg?" + "group_id=" + group_id + "&" + "message=" + "[CQ:at,qq=" + user_id + "]" + URLEncoder.encode(Message);
        String result;
        try {
            result = get(GETBody);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.printf("向群号为%d的群聊发送了at类型消息：%s，被at的QQ号为：%d，请求的结果为： %s\n",group_id, Message, user_id, result);
    }
    public static void SendGroupMessage(long group_id, String Message) {
        String GETBody = qqapi_url + "send_group_msg?" + "group_id=" + group_id + "&" + "message=" + URLEncoder.encode(Message);
        String result;
        try {
            result = get(GETBody);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.printf("向群号为%d的群聊发送了消息：%s，请求的结果为：%s\n",group_id, Message, result);
    }

    public static void SendGroupMessage(String group_id, String Message) {
        String GETBody = qqapi_url + "send_group_msg?" + "group_id=" + group_id + "&" + "message=" + URLEncoder.encode(Message);
        String result;
        try {
            result = get(GETBody);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.printf("向群号为%s的群聊发送了消息：%s，请求的结果为：%s\n",group_id, Message, result);
    }

    public static void SendPrivateMessage(long user_id, String Message) {
        String url = qqapi_url + "send_msg?message_type=private&";
        String GETBody = url + "user_id=" + user_id + "&" + "message=" + URLEncoder.encode(Message);
        System.out.printf("向QQ号为 %d 的好友发送了消息：%s，GO-CQHTTP返回的结果为：", user_id, Message);
        String result;
        try {
            result = get(GETBody);
            System.out.println(result);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String get(String url) throws Exception {
        String content = null;
        URLConnection urlConnection = new URL(url).openConnection();
        HttpURLConnection connection = (HttpURLConnection) urlConnection;
        connection.setRequestMethod("GET");
        //连接
        connection.connect();
        //得到响应码
        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader
                    (connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder bs = new StringBuilder();
            String l;
            while ((l = bufferedReader.readLine()) != null) {
                bs.append(l).append("\n");
            }
            content = bs.toString();
        }
        return content;
    }

    public String SendMCCommandMCSM(String command) {
        String url = mcapi_url + "/api/protected_instance/command" +
                    "?" +
                    "&" + "uuid=" + mcapi_uid +
                    "&" + "remote_uuid=" + mcapi_gid +
                    "&" + "apikey=" + mcapikey +
                    "&" + "command=" + URLEncoder.encode(command);
        try {
            JSONObject jsonObject = JSONObject.parseObject(get(url));
            int result = jsonObject.getIntValue("status");
            System.out.printf("尝试向MC服务端发送命令：%s，结果为：%d\n", command, result);
            return jsonObject.getString("status");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
        public String SendMCCommandRCON(String command){
            try {
                String rconHost = mcapi_url.replace("http://", "").split(":")[0];
                net.kronos.rkon.core.Rcon rcon = new Rcon(rconHost, rconPort, rconPassword.getBytes(StandardCharsets.UTF_8));
                String ret = rcon.command(command);
                System.out.printf("在RCON模式下发送了命令：%s，返回：%s\n", command, ret);
                rcon.disconnect();
                return ret;
            } catch (IOException | AuthenticationException e) {
                throw new RuntimeException(e);
            }

        }
    public void SendMCMessage(String Message, String sender, boolean isJavaEdition, boolean enabledRcon) {
        String jsonData;
        String Msg2 = "*<" + sender + ">" + Message;
        Msg2 = new String(Msg2.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        if (isJavaEdition) jsonData = "{\"text\":\"" + Msg2 + "\",\"color\":\"yellow\"}";
        else jsonData = "{\"rawtext\":[{\"text\":\"" + Msg2 + "\"}]}";
        String command = "tellraw @a " + jsonData;
        if (enabledRcon) SendMCCommandRCON(command);
        else SendMCCommandMCSM(command);
    }
    public void SendGPTMessage(String Message, boolean isJavaEdition, boolean enabledRcon) {
        Message = new String(Message.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        String jsonData;
        if (isJavaEdition) jsonData = "[{\"text\":\"[Bot]\",\"bold\":\"true\",\"color\":\"blue\"},{\"text\":\"" + Message + "\",\"color\":\"green\"}]";
        else jsonData = "{\"rawtext\":[{\"text\":\"[GPT]" + Message + "\"}]}";
        String command = "tellraw @a " + jsonData;
        if (enabledRcon) SendMCCommandRCON(command);
        else SendMCCommandMCSM(command);
    }
}
