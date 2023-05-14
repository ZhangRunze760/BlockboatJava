package org.zhangrunze.Blockboat;

class MCMessageProcessing {
    private static boolean isJavaEdition;
    private static String qqapi_url;
    private static String mcapi_url;
    private static String mcapi_uid;
    private static String mcapi_gid;
    private static String mcapikey;
    private static String qqgroup_id;
    static boolean enabledRcon;
    static int rconPort;
    static String rconPassword;

    public MCMessageProcessing(boolean isJavaEdition, String qqapi_url, String mcapi_url, String mcapi_uid, String mcapi_gid, String mcapikey, String qqgroup_id, boolean enabledRcon, int rconPort, String rconPassword) {
        MCMessageProcessing.isJavaEdition = isJavaEdition;
        MCMessageProcessing.qqapi_url = qqapi_url;
        MCMessageProcessing.mcapi_url = mcapi_url;
        MCMessageProcessing.mcapi_uid = mcapi_uid;
        MCMessageProcessing.mcapi_gid = mcapi_gid;
        MCMessageProcessing.mcapikey = mcapikey;
        MCMessageProcessing.qqgroup_id = qqgroup_id;
        MCMessageProcessing.enabledRcon = enabledRcon;

        MCMessageProcessing.rconPort = rconPort;
        MCMessageProcessing.rconPassword = rconPassword;

        if (enabledRcon) System.out.println("MC消息发送：RCON模式。");
        else System.out.println("MC消息发送：MCSM模式。");
    }

    public final SendMessage sendMessage = new SendMessage(qqapi_url, mcapi_url, mcapi_uid, mcapi_gid, mcapikey, enabledRcon, rconPort, rconPassword);

    public void Process(String lastLine) {
        if (lastLine.contains("[Server thread/INFO]")) {
            if (lastLine.contains("[Server thread/INFO]: [Not Secure] <")) {
                String Message = lastLine.substring(46);
                String raw_message = Message.replace("<", "").split("> ")[1];
                if (raw_message.startsWith("!!")) {
                    String Msg2 = RequestProcessing.GetMessage(raw_message.replace("!!", ""));
                    sendMessage.SendGPTMessage(Msg2, isJavaEdition, enabledRcon);
                } else SendMessage.SendGroupMessage(qqgroup_id, Message);
            } else if (lastLine.contains("[Server thread/INFO]: There are")) {
                String Message = lastLine.substring(33);
                SendMessage.SendGroupMessage(qqgroup_id, Message);
            } else if (lastLine.contains(" joined the game")) {
                String Message = lastLine.substring(33, lastLine.length() - 15) + "加入了游戏";
                SendMessage.SendGroupMessage(qqgroup_id, Message);
            } else if (lastLine.contains(" left the game")) {
                String Message = lastLine.substring(33, lastLine.length() - 13) + "退出了游戏";
                SendMessage.SendGroupMessage(qqgroup_id, Message);
            }
        } else if (lastLine.contains("/INFO]: [Textile Backup] Starting backup")) {
            String Message = "开始备份...可能会出现微小卡顿。";
            SendMessage.SendGroupMessage(qqgroup_id, Message);
        } else if (lastLine.contains("/INFO]: [Textile Backup] Compression took:")) {
            String Message = "备份完成！花费时间：" + lastLine.substring(70);
            SendMessage.SendGroupMessage(qqgroup_id, Message);
        }
    }
}
