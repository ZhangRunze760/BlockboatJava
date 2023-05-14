package org.zhangrunze.Blockboat;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.*;
import java.io.*;
import java.net.URLEncoder;
import java.util.Objects;
import org.apache.commons.io.*;

public class RequestProcessing {
    private static boolean isJavaEdition;
    private static String qqapi_url;
    private static String mcapi_url;
    private static String mcapi_uid;
    private static String mcapi_gid;
    private static String mcapikey;
    private static final BindManager bindManager = new BindManager();
    private static String OPENAI_API;
    private static String qqgroup_id;
    static boolean enabledRcon;
    static int rconPort;
    static String rconPassword;

    public RequestProcessing(String qqgroup_id, String qqapi_url, String mcapi_url, String mcapi_uid, String mcapi_gid, String mcapikey, String OPENAI_API, boolean isJavaEdition, boolean enabledRcon, int rconPort, String rconPassword) {
        RequestProcessing.qqapi_url = qqapi_url;
        RequestProcessing.mcapi_url = mcapi_url;
        RequestProcessing.mcapi_uid = mcapi_uid;
        RequestProcessing.mcapi_gid = mcapi_gid;
        RequestProcessing.mcapikey = mcapikey;
        RequestProcessing.OPENAI_API = OPENAI_API;
        RequestProcessing.isJavaEdition = isJavaEdition;
        RequestProcessing.qqgroup_id = qqgroup_id;
        RequestProcessing.enabledRcon = enabledRcon;
        RequestProcessing.rconPassword = rconPassword;
        RequestProcessing.rconPort = rconPort;
    }

    static long group_id;
    static long user_id;
    static String Message;

    public static void process(String requestBody) {
        String Msg2;
        SendMessage sendMessage = new SendMessage(qqapi_url, mcapi_url, mcapi_uid, mcapi_gid, mcapikey, enabledRcon, rconPort, rconPassword);
        try {
            String fileContent = FileUtils.readFileToString(new File("oplist.txt"), "UTF-8");
            JSONObject jsonObject = JSON.parseObject(requestBody);
            System.out.printf("收到了新的消息，消息类型为：%s，发来的JSON字符串为：%s\n", jsonObject.getString("message_type"), requestBody);
            String RAWMessage = jsonObject.getString("message");

            if (Objects.equals(jsonObject.getString("message_type"), "group")) {

                group_id = jsonObject.getLong("group_id");
                user_id = jsonObject.getLong("user_id");
                Message = jsonObject.getString("message");

                if (RAWMessage.startsWith("!!") || RAWMessage.startsWith("！！")) {
                    System.out.printf("在群号为：%d的群聊收到来自QQ号为：%d用户对OPENAI API的请求，消息为：%s\n", group_id, user_id, Message.replace("！！ ", ""));
                    SendMessage.SendGroupMessage(group_id, user_id, GetMessage(Message.replace("！！ ", "")).replace("!!", ""));
                }

                else if (RAWMessage.startsWith("sudo ") && Long.toString(jsonObject.getLong("group_id")).equals(qqgroup_id)) {
                    if (Objects.equals(Message, "sudo list")) {
                        if (enabledRcon) sendMessage.SendGroupMessage(group_id, sendMessage.SendMCCommandRCON("list"));
                        else sendMessage.SendMCCommandMCSM("list");
                        return;
                    }
                    else if (fileContent.contains(Long.toString(user_id))) {
                        String command = RAWMessage.replace("sudo ", "");

                        if (command.startsWith("bindcontrol ")) {
                            String rawCom = command.replace("bindcontrol ", "");
                            if (rawCom.startsWith("add ")) {
                                String arg1 = rawCom.replace("add ", "").split(" ")[0];
                                String arg2 = rawCom.replace("add ", "").split(" ")[1];
                                if (bindManager.bind(arg1, arg2)) sendMessage.SendGroupMessage(group_id, "绑定成功！");
                                else sendMessage.SendGroupMessage(group_id, "绑定失败，QQ号已经绑定过了MCID。");
                                return;
                            }
                            else if (rawCom.startsWith("removeId ")) {
                                String id = rawCom.replace("removeId ", "");
                                if (!bindManager.unbindById(id)) sendMessage.SendGroupMessage(group_id, "解绑失败，QQ未绑定过MCID。");
                                else sendMessage.SendGroupMessage(group_id, "解绑成功！");
                                return;
                            }
                            else if (rawCom.startsWith("removeMCID ")) {
                                String name = rawCom.replace("removeMCID ", "");
                                if (!bindManager.unbindByName(name)) sendMessage.SendGroupMessage(group_id, "绑解失败，MCID未绑定过QQ号。");
                                else sendMessage.SendGroupMessage(group_id, "解绑成功！");
                                return;
                            }
                            else if (rawCom.startsWith("printAll")) {
                                String Out = bindManager.printAll();
                                sendMessage.SendGroupMessage(group_id, Out);
                            }
                        }

                        else {
                            String result;
                            if (enabledRcon) {
                                result = sendMessage.SendMCCommandRCON(command);
                                SendMessage.SendGroupMessage(group_id, result);
                            }
                                else {
                                result = sendMessage.SendMCCommandMCSM(command);
                                if (Objects.equals(result, "200")) {
                                    SendMessage.SendGroupMessage(group_id, "发送成功！");
                                } else {
                                    SendMessage.SendGroupMessage(group_id, "发送失败。。。");
                                }
                                return;
                            }
                        }
                    } else {
                        SendMessage.SendGroupMessage(group_id, "权限不够！");
                        return;
                    }

                } else if (RAWMessage.startsWith("bind ")) {
                    if (bindManager.bind(Long.toString(user_id), RAWMessage.replace("bind ", ""))) {
                        SendMessage.SendGroupMessage(group_id, "绑定成功！");
                    }
                    else {
                        SendMessage.SendGroupMessage(group_id, "绑定失败，已经绑定过MCID的QQ号无法再绑定第二个。");
                    }
                    return;
                } else if (RAWMessage.startsWith("unbind")) {
                    if (bindManager.unbindById(Long.toString(user_id))) {
                        SendMessage.SendGroupMessage(group_id, "解绑成功！");
                    }
                    else {
                        SendMessage.SendGroupMessage(group_id, "解绑失败，QQ号没有绑定MCID。");
                    }
                    return;
                }

                else if (Long.toString(jsonObject.getLong("group_id")).equals(qqgroup_id)) {
                    String sender;
                    if (Message.contains("[CQ:")) Msg2 = CQProcess(Message);
                    else Msg2 = Message;
                    if (bindManager.getNameById(Long.toString(user_id)) != null && !bindManager.getNameById(Long.toString(user_id)).equals("null"))
                        sender = bindManager.getNameById(Long.toString(user_id));
                    else
                        sender = JSON.parseObject(jsonObject.getString("sender")).getString("card").replace("\n", "").replace(" ", "");
                    sendMessage.SendMCMessage(Msg2, sender, isJavaEdition, enabledRcon);
                    return;
                }
            } else if (Objects.equals(jsonObject.getString("message_type"), "private")) {
                user_id = jsonObject.getLong("user_id");
                Message = jsonObject.getString("message");
                SendMessage.SendPrivateMessage(user_id, GetMessage(Message));
                return;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String GetMessage(String Message) {
        final String API_KEY = OPENAI_API;
        final String MODEL_ID = "gpt-3.5-turbo";
        System.out.printf("向ID为 %s 的机器人模型发送了消息：%s，返回的JSON字符串为：", MODEL_ID, Message);
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(mediaType, "{\"model\": \"" + MODEL_ID + "\"," + "\"messages\": [{\"role\": \"user\", \"content\": \"" + URLEncoder.encode(Message) + "\"}],\"temperature\": 0.7}");
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .method("POST", body)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + API_KEY)
                .build();
        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            String JsonStr = response.body().string();
            JSONObject jsonObject = JSON.parseObject(JsonStr);
            System.out.println(JsonStr);
            return JSON.parseObject(JSON.parseObject(jsonObject.getString("choices").replace("[", "").replace("]", "")).getString("message")).getString("content");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String CQProcess(String str) {
        String type = str.substring(1, str.indexOf(','));
        switch (type) {
            case "CQ:reply" -> {
                String id = (((str.split("\\[CQ:reply,id="))[1]).split("\\[CQ:at,qq="))[2].split("] ")[0];
                String raw_message = (((str.split("\\[CQ:reply,id="))[1]).split("\\[CQ:at,qq="))[2].split("] ")[1];
                if (bindManager.IsIdBind(id)) return ("【回复：" + bindManager.getNameById(id) + "】" + raw_message);
                else return ("【回复：" + id + "】" + raw_message);
            }
            case "CQ:at" -> {
                String at_qqid = ((str.split("\\[CQ:at,qq="))[1].split("] "))[0];
                String raw_message2 = ((str.split("\\[CQ:at,qq="))[1].split("] "))[1];
                if (bindManager.IsIdBind(at_qqid)) return ("@" + bindManager.getNameById(at_qqid) + "：" + raw_message2);
                else return ("@" + at_qqid + "：" + raw_message2);
            }
            case "CQ:image" -> {
                return "【图片】";
            }
            default -> {
                return type.replace("CQ:", "");
            }
        }
    }
}

