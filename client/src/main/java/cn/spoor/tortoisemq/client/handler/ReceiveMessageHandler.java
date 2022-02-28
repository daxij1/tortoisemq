package cn.spoor.tortoisemq.client.handler;

import cn.spoor.tortoisemq.common.message.MessageEntity;
import cn.spoor.tortoisemq.common.message.MessageType;
import cn.spoor.tortoisemq.consumer.Consumer;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/13 09:46
 * @description：收消息处理器
 */
public class ReceiveMessageHandler extends SimpleChannelInboundHandler<MessageEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveMessageHandler.class);

    private Consumer consumer;

    public ReceiveMessageHandler(Consumer consumer) {
        this.consumer = consumer;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MessageEntity messageEntity) throws Exception {
        if (MessageType.READ_RESPONSE.equals(messageEntity.getType())) {
            consumer.handleMessage((JSONObject) messageEntity.getData());
        } else {
            LOGGER.info("其他消息:{}", messageEntity);
        }
    }

}
