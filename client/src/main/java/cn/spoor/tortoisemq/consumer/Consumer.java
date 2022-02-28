package cn.spoor.tortoisemq.consumer;

import cn.spoor.tortoisemq.adaptor.DefaultClientAdaptor;
import cn.spoor.tortoisemq.client.handler.ReceiveMessageHandler;
import cn.spoor.tortoisemq.common.message.MessageEntity;
import cn.spoor.tortoisemq.common.message.MessageFactory;
import cn.spoor.tortoisemq.common.threadfactory.ThreadFactoryImpl;
import cn.spoor.tortoisemq.message.MessageListener;
import com.alibaba.fastjson.JSONObject;
import io.netty.channel.Channel;
import io.netty.channel.socket.SocketChannel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/16 13:20
 * @description：Consumer
 */
public class Consumer extends DefaultClientAdaptor {

    private final ExecutorService executors = Executors.newCachedThreadPool();

    private final String topic;

    protected MessageListener listener;

    public Consumer(String zookeeperConnect, String topic) {
        super(zookeeperConnect);
        this.topic = topic;
    }

    public void registerMessageListener(MessageListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (listener == null) {
            LOGGER.info("未注册listener");
            System.exit(-1);
        }
        // 开启线程每隔1s钟向服务端拉取消息
        Executors.newSingleThreadScheduledExecutor(new ThreadFactoryImpl("pullMessageThread"))
                .scheduleAtFixedRate(this::pullMessage, 0, 1, TimeUnit.SECONDS);
    }

    private void pullMessage() {
        for (Channel channel : brokerChannels) {
            executors.execute(() -> {
                MessageEntity messageEntity = MessageFactory.getPullMessage(topic);
                channel.writeAndFlush(messageEntity);
            });
        }
    }

    @Override
    public void addHandler(SocketChannel channel) {
        channel.pipeline().addLast("receiveMessageHandler", new ReceiveMessageHandler(this));
    }

    public void handleMessage(JSONObject message) {
        if (listener != null) {
            listener.consumeMessage(message);
        }
    }

}
