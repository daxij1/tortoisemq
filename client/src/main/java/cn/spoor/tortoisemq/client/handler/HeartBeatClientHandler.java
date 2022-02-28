package cn.spoor.tortoisemq.client.handler;

import cn.spoor.tortoisemq.adaptor.DefaultClientAdaptor;
import cn.spoor.tortoisemq.common.config.BrokerConfig;
import cn.spoor.tortoisemq.common.message.MessageEntity;
import cn.spoor.tortoisemq.common.message.MessageFactory;
import cn.spoor.tortoisemq.common.threadfactory.ThreadFactoryImpl;
import cn.spoor.tortoisemq.common.util.BrokerConfigUtil;
import cn.spoor.tortoisemq.retry.RetryPolicy;
import cn.spoor.tortoisemq.retry.impl.DefaultRetryPolicy;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/13 14:22
 * @description：心跳、断线重连处理器
 */
public class HeartBeatClientHandler extends SimpleChannelInboundHandler<MessageEntity> {

    private final DefaultClientAdaptor client;
    private final Bootstrap bootstrap;
    private int retries = 0;
    private final RetryPolicy retryPolicy = new DefaultRetryPolicy(2000, 10, 10000);

    private static final Logger LOGGER = LoggerFactory.getLogger(HeartBeatClientHandler.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("ProducerHeartScheduledThread"));

    public HeartBeatClientHandler(DefaultClientAdaptor client, Bootstrap bootstrap) {
        this.client = client;
        this.bootstrap = bootstrap;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        retries = 0;
        //间隔3秒发送心跳
        executor.scheduleAtFixedRate(() -> ctx.writeAndFlush(MessageFactory.getHeartRequestMessage()), 0, 3, TimeUnit.SECONDS);
        ctx.writeAndFlush(MessageFactory.getHeartRequestMessage());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, MessageEntity messageEntity) throws Exception {
        //如果是心跳响应信息，不处理
        if (MessageFactory.getHeartResponseMessage().equals(messageEntity)) {
            return;
        }
        //否则传递到下一个handler
        //调用该方法channelHandlerContext才会传递到下一个handler，否则下一个handler的read方法不会被调用
        channelHandlerContext.fireChannelRead(messageEntity);
    }

    /**
     * broker断线有两种情况
     * 一种是broker挂了，zk相关的注册信息也没了，这种情况不用尝试重连
     * 一种是broker与客户端网络不通，但与zk是正常的，这种情况才需要重连
     * 如果broker与zk网络不通了，与客户端正常，这种情况producer不断开与broker的连接
     * */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String ip = socketAddress.getHostString();
        int port = socketAddress.getPort();
        if (retries == 0) {
            LOGGER.error("TCP失去连接，ip：{}，port：{}", ip, port);
            client.getBrokerChannels().remove(ctx.channel());
            ctx.close();
        }
        // 判断是否需要重连，zk存在节点则重连
        AtomicBoolean needReconnect = new AtomicBoolean(false);
        List<BrokerConfig> brokerConfigList = BrokerConfigUtil.requireBrokerInfo(client.getZkCurator());
        brokerConfigList.forEach(item -> {
            if (item.getBrokerIp().equals(ip) && item.getListenPort() == port){
                needReconnect.set(true);
            }
        });
        if (needReconnect.get()) {
            doReconnect(ip, port, ctx);
        }
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        executor.shutdown();
        ctx.executor().shutdownGracefully();
        ctx.close();
    }

    private void doReconnect(String ip, int port, ChannelHandlerContext ctx) {
        boolean allowRetry = retryPolicy.allowRetry(retries);
        if (allowRetry) {
            long sleepTimeMs = retryPolicy.getSleepTimeMs(retries);
            LOGGER.info("{}ms后开始第{}次重连，ip：{}，port：{}", sleepTimeMs, ++retries, ip, port);
            final EventLoop eventLoop = ctx.channel().eventLoop();
            eventLoop.schedule(() -> {
                ChannelFuture channelFuture = bootstrap.connect(ip, port);
                channelFuture.addListener(future -> {
                    if (!future.isSuccess()) {
                        LOGGER.info("重连失败，ip：{}，port：{}", ip, port);
                        doReconnect(ip, port, ctx);
                    } else {
                        LOGGER.info("重连成功，ip：{}，port：{}", ip, port);
                        client.getBrokerChannels().add(channelFuture.channel());
                    }
                });
            }, sleepTimeMs, TimeUnit.MILLISECONDS);
        } else {
            executor.shutdown();
            ctx.executor().shutdownGracefully();
        }
    }

}
