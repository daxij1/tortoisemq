package cn.spoor.tortoisemq.adaptor;


import cn.spoor.tortoisemq.client.Client;
import cn.spoor.tortoisemq.client.handler.HeartBeatClientHandler;
import cn.spoor.tortoisemq.client.handler.ReceiveMessageHandler;
import cn.spoor.tortoisemq.common.config.BrokerConfig;
import cn.spoor.tortoisemq.common.message.MessageDecoder;
import cn.spoor.tortoisemq.common.message.MessageEncoder;
import cn.spoor.tortoisemq.common.message.MessageEntity;
import cn.spoor.tortoisemq.common.util.BrokerConfigUtil;
import cn.spoor.tortoisemq.common.util.CommonConstants;
import cn.spoor.tortoisemq.listener.BrokerAwareToProducerListener;
import cn.spoor.tortoisemq.message.MessageListener;
import com.alibaba.fastjson.JSONObject;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/16 13:45
 * @description：客户端适配器
 * 无论是producer，还是consumer，都有以下共性：
 * 1.都需要维护一个broker的信息列表，一个broker的channel列表，当zk变更时从而进行变更
 * 2.都需要与broker建立连接，也就需要编解码、心跳检测等一系列handler
 */
public abstract class DefaultClientAdaptor implements Client {

    protected Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    // broker信息列表
    protected final List<BrokerConfig> brokerConfigList = Collections.synchronizedList(new ArrayList<>());

    // broker的channel
    protected final List<Channel> brokerChannels = Collections.synchronizedList(new ArrayList<>());

    // zk操作客户端
    protected CuratorFramework zkCurator;

    // zk连接地址
    protected String zookeeperConnect;

    public DefaultClientAdaptor(String zookeeperConnect) {
        this.zookeeperConnect = zookeeperConnect;
    }

    public void initialize() {
        // 1.初始化curator
        // 重试策略（重试次数3，重试间隔时间5s）
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(5000, 3);
        LOGGER.info("zk连接到{}...", zookeeperConnect);
        // 建立会话
        zkCurator = CuratorFrameworkFactory.builder()
                .connectString(zookeeperConnect)
                .sessionTimeoutMs(5000)  // 会话超时时间
                .connectionTimeoutMs(15000) // 连接超时时间
                .retryPolicy(retryPolicy)
                .namespace("tortoisemq") // 隔离名称，会在zkServer创建该根目录
                .build();
        zkCurator.start();
        // 2.获取broker节点信息
        try {
            brokerConfigList.addAll(BrokerConfigUtil.requireBrokerInfo(zkCurator));
        } catch (Exception e) {
            LOGGER.info("获取zk节点信息失败");
            e.printStackTrace();
            System.exit(-1);
        }
        // 3.建立broker的channel
        List<ChannelFuture> channelFutureList = new ArrayList<>();
        for (BrokerConfig brokerConfig : brokerConfigList) {
            ChannelFuture channelFuture = connectByBrokerConfig(brokerConfig);
            channelFutureList.add(channelFuture);
        }
        for (ChannelFuture channelFuture : channelFutureList) {
            try {
                brokerChannels.add(channelFuture.sync().channel());
            } catch (InterruptedException e) {
                LOGGER.info("连接broker异常，程序退出");
                e.printStackTrace();
                System.exit(-1);
            }
        }
        // 4.建立zk监听(这一步要放在第3步后面，因为开启监听后有可能会对brokerConfigList元素进行增删，由于第3步
        // 有对brokerConfigList的遍历操作，可能引发ConcurrentModificationException，详见itertors.next()的checkForComodification)
        // 3.5.8的zk版本并不支持CuratorCache的方式监听，详见Compatibility的静态代码块，这里用废弃的方式
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkCurator, CommonConstants.BROKER_ID, true);
        pathChildrenCache.getListenable().addListener(new BrokerAwareToProducerListener(this));
        try {
            pathChildrenCache.start(true);
        } catch (Exception e) {
            LOGGER.info("启动zk监听失败");
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private ChannelFuture connectByBrokerConfig(BrokerConfig brokerConfig) {
        EventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        //加入处理器
                        channel.pipeline()
                                .addLast("messageDecoder", new MessageDecoder(MessageEntity.class))
                                .addLast("messageEncoder", new MessageEncoder())
                                .addLast(new ReadTimeoutHandler(50))
                                .addLast("heartBeatClientHandler", new HeartBeatClientHandler(DefaultClientAdaptor.this, bootstrap));
                        addHandler(channel);
                    }
                });
        String ip = brokerConfig.getBrokerIp();
        int port = brokerConfig.getListenPort();
        return bootstrap.connect(ip, port);
    }

    //模板方法，Consumer有消费的handler，可以继续添加
    protected void addHandler(SocketChannel channel) {

    }

    @Override
    public void handleNodeAdd(BrokerConfig brokerInfo) {
        AtomicBoolean needAdd = new AtomicBoolean(true);
        brokerConfigList.forEach(item -> {
            if (item.getBrokerId().equals(brokerInfo.getBrokerId())) {//如果已存在则不加入
                needAdd.set(false);
            }
        });
        if (needAdd.get()) {
            ChannelFuture channelFuture = connectByBrokerConfig(brokerInfo);
            try {
                Channel channel = channelFuture.sync().channel();
                brokerConfigList.add(brokerInfo);
                brokerChannels.add(channel);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void handleNodeDel(BrokerConfig brokerInfo) {
        brokerConfigList.removeIf(config -> config.getBrokerId().equals(brokerInfo.getBrokerId()));
        //broker与zk连接断开，不移除channels，只有当broker与producer或consumer断开时才移除，见HeartBeatClientHandler.channelInactive
    }

    public List<BrokerConfig> getBrokerConfigList() {
        return brokerConfigList;
    }

    public List<Channel> getBrokerChannels() {
        return brokerChannels;
    }

    public CuratorFramework getZkCurator() {
        return zkCurator;
    }

    public void setZkCurator(CuratorFramework zkCurator) {
        this.zkCurator = zkCurator;
    }

    public String getZookeeperConnect() {
        return zookeeperConnect;
    }

    public void setZookeeperConnect(String zookeeperConnect) {
        this.zookeeperConnect = zookeeperConnect;
    }

}
