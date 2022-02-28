package cn.spoor.tortoisemq.server.handler;

import cn.spoor.tortoisemq.common.message.MessageEntity;
import cn.spoor.tortoisemq.common.message.MessageFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @author ：spoor
 * @date ：Created in 2022/2/19 22:06
 * @description：心跳检测，通过读超时事件，即当客户端(生产者、消费者)一段时间未发送心跳时，断开连接
 */
public class HeartBeatServerHandler extends SimpleChannelInboundHandler<MessageEntity> {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatServerHandler.class);

    //读空闲次数记录
    private final Map<ChannelHandlerContext, Integer> readIdleMap = new HashMap<>();

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            String eventType = null;
            Integer readIdleTimes = readIdleMap.get(ctx);
            switch (event.state()) {
                case READER_IDLE:
                    eventType = "读空闲";
                    readIdleTimes++; // 读空闲的计数加1
                    readIdleMap.put(ctx, readIdleTimes);
                    break;
                case WRITER_IDLE:
                    eventType = "写空闲";
                    // 不处理
                    break;
                case ALL_IDLE:
                    eventType = "读写空闲";
                    // 不处理
                    break;
            }
            LOGGER.info("{}超时事件:{}", ctx.channel().remoteAddress(), eventType);
            if (readIdleTimes > 2) {
                LOGGER.info("{}读空闲超过3次，关闭连接，释放资源", ctx.channel().remoteAddress());
                readIdleMap.remove(ctx);
                ctx.channel().close();
            }
        }

    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MessageEntity messageEntity) throws Exception {
        // 收到心跳信息，进行回复
        if (MessageFactory.getHeartRequestMessage().equals(messageEntity)) {
            channelHandlerContext.channel().writeAndFlush(MessageFactory.getHeartResponseMessage());
        } else {
            // 传递到下一个handler
            channelHandlerContext.fireChannelRead(messageEntity);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常断开连接，清除ctx计数器
        readIdleMap.remove(ctx);
        LOGGER.info("连接发生异常，原因：{}", cause.getMessage());
    }

}
