package cn.spoor.tortoisemq.common.message;

import cn.hutool.core.util.IdUtil;
import cn.spoor.tortoisemq.common.util.CommonConstants;
import com.alibaba.fastjson.JSONObject;

import java.util.Random;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/13 13:59
 * @description：MessageFactory
 */
public class MessageFactory {

    //心跳请求message
    private static final MessageEntity heartRequestMessage = new MessageEntity();
    //心跳响应message
    private static final MessageEntity heartResponseMessage = new MessageEntity();

    static {
        heartRequestMessage.setType(MessageType.HEART);
        heartRequestMessage.setData("Heartbeat Packet");
        heartResponseMessage.setType(MessageType.HEART);
        heartResponseMessage.setData("ok");
    }

    public static MessageEntity getHeartRequestMessage() {
        return heartRequestMessage;
    }

    public static MessageEntity getHeartResponseMessage() {
        return heartResponseMessage;
    }

    /**
    * 构造发送的topic消息实体
    * */
    public static MessageEntity consMessage(String topic, Object data) {
        MessageEntity message = new MessageEntity();
        message.setType(MessageType.WRITE);
        JSONObject msgBody = new JSONObject();
        msgBody.put(CommonConstants.TOPIC, topic);
        msgBody.put(CommonConstants.MSG, data);
        String msgId = IdUtil.createSnowflake(new Random().nextInt(32), 1).nextIdStr();
        msgBody.put(CommonConstants.MSGID, msgId);
        message.setData(msgBody);
        return message;
    }

    /**
     * 拉取消息发送的message实体
     */
    public static MessageEntity getPullMessage(String topic) {
        MessageEntity message = new MessageEntity();
        message.setType(MessageType.READ);
        message.setData(topic);
        return message;
    }

    /**
     * 响应consumer发送的topic消息实体
     * */
    public static MessageEntity consResponseMessage(Object data) {
        MessageEntity message = new MessageEntity();
        message.setType(MessageType.READ_RESPONSE);
        message.setData(data);
        return message;
    }

}
