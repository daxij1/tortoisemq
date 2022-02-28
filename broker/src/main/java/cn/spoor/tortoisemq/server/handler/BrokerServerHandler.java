package cn.spoor.tortoisemq.server.handler;

import cn.hutool.core.collection.CollUtil;
import cn.spoor.tortoisemq.common.message.MessageEntity;
import cn.spoor.tortoisemq.common.message.MessageFactory;
import cn.spoor.tortoisemq.common.message.MessageType;
import cn.spoor.tortoisemq.common.util.CommonConstants;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/9 20:34
 * @description：BrokerServerHandler
 */
public class BrokerServerHandler extends SimpleChannelInboundHandler<MessageEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerServerHandler.class);

    private final Map<String, BlockingQueue<JSONObject>> messageQueueMap;

    public BrokerServerHandler(Map<String, BlockingQueue<JSONObject>> messageQueueMap) {
        this.messageQueueMap = messageQueueMap;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("客户端{}已连接", ctx.channel().remoteAddress());
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        LOGGER.info("客户端{}已断开", ctx.channel().remoteAddress());
        ctx.close();
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.info("连接发生异常，原因为：{}",cause.getMessage());
        LOGGER.info("断开连接{}", ctx.channel().remoteAddress());
        ctx.close();
        super.exceptionCaught(ctx, cause);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MessageEntity messageEntity) throws Exception {
        //生产者发的写消息写入相关topic队列
        if (messageEntity.getType().equals(MessageType.WRITE)) {
            LOGGER.info("收到生产者消息：{}", messageEntity);
            JSONObject message = (JSONObject) messageEntity.getData();
            String topic = message.getString(CommonConstants.TOPIC);
            BlockingQueue<JSONObject> messageQueue = messageQueueMap.get(topic);
            if (messageQueue == null) {
                messageQueue = new LinkedBlockingQueue<>();
                messageQueueMap.put(topic, messageQueue);
            }
            messageQueue.add(message);
        } else if (messageEntity.getType().equals(MessageType.READ)) {//消费者发的读消息
            String topic = (String) messageEntity.getData();
            BlockingQueue<JSONObject> messageQueue = messageQueueMap.get(topic);
            if (!CollUtil.isEmpty(messageQueue) ) {
                JSONObject message = messageQueue.poll(5, TimeUnit.MILLISECONDS);
                if (message != null) {
                    channelHandlerContext.writeAndFlush(MessageFactory.consResponseMessage(message));
                }
            }
        }
    }

}
