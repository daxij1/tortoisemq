package cn.spoor.tortoisemq.server;

import cn.spoor.tortoisemq.common.config.BrokerConfig;
import cn.spoor.tortoisemq.common.message.MessageDecoder;
import cn.spoor.tortoisemq.common.message.MessageEncoder;
import cn.spoor.tortoisemq.common.message.MessageEntity;
import cn.spoor.tortoisemq.common.threadfactory.ThreadFactoryImpl;
import cn.spoor.tortoisemq.server.handler.BrokerServerHandler;
import cn.spoor.tortoisemq.server.handler.HeartBeatServerHandler;
import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author ：spoor
 * @date ：Created in 2022/2/19 21:47
 * @description：BrokerRemoteServer
 */
public class BrokerRemoteServer {

    private final ServerBootstrap serverBootstrap;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    // 消费者列表
    private final List<Channel> consumers = Collections.synchronizedList(new ArrayList<>());
    // 生产者列表
    private final List<Channel> producers = Collections.synchronizedList(new ArrayList<>());
    // topic和消息队列映射
    private final Map<String, BlockingQueue<JSONObject>> messageQueueMap = new ConcurrentHashMap<>();

    public BrokerRemoteServer(BrokerConfig config) {
        this.bossGroup = new NioEventLoopGroup(1, new ThreadFactoryImpl("bossGroupThread"));
        this.workerGroup = new NioEventLoopGroup(config.getServerThreadNum(), new ThreadFactoryImpl("workerGroupThread"));
        this.serverBootstrap = new ServerBootstrap();
        serverBootstrap.localAddress(config.getBrokerIp(), config.getListenPort());
    }

    public void start() {
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childHandler(new ChannelInitializer<SocketChannel>() {//创建通道初始化对象，设置初始化参数
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        // 对workerGroup的SocketChannel设置处理器
                        ch.pipeline()
                                // 设置解码器
                                .addLast("messageDecoder", new MessageDecoder(MessageEntity.class))
                                // 设置编码器
                                .addLast("messageEncoder", new MessageEncoder())
                                // 超过50s未传输数据就断开连接
                                .addLast(new ReadTimeoutHandler(50))
                                // 设置5s未收到心跳信息，触发读空闲事件
                                .addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS))
                                // 处理读空闲事件
                                .addLast(new HeartBeatServerHandler())
                                // 处理生产者的写消息和消费者的读消息事件
                                .addLast(new BrokerServerHandler(messageQueueMap))
                        ;
                    }
                });
        try {
            // 此处是异步的，返回ChannelFuture
            serverBootstrap.bind().sync();
        } catch (InterruptedException e) {
            throw new RuntimeException("serverBootstrap.bind().sync() InterruptedException", e);
        }
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }

}
