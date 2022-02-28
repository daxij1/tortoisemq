package cn.spoor.tortoisemq;

import cn.spoor.tortoisemq.common.config.BrokerConfig;
import cn.spoor.tortoisemq.common.util.BrokerConfigUtil;
import cn.spoor.tortoisemq.server.BrokerRemoteServer;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/9 11:20
 * @description：Broker总控器
 */
public class BrokerController {

    // broker配置
    private BrokerConfig brokerConfig;
    // zk客户端
    private CuratorFramework zkCurator;
    // broker网络服务
    private BrokerRemoteServer bootServer;

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerController.class);

    public BrokerController(String configFilePath) {
        this.brokerConfig = BrokerConfigUtil.loadConfig(configFilePath);
    }

    public void initialize() {
        // 1.初始化curator
        // 重试策略（重试次数3，重试间隔时间5s）
        RetryPolicy retryPolicy = new ExponentialBackoffRetry(5000, 3);
        LOGGER.info("zk连接到{}...", brokerConfig.getZookeeperConnect());
        // 建立会话
        zkCurator = CuratorFrameworkFactory.builder()
                .connectString(brokerConfig.getZookeeperConnect())
                .sessionTimeoutMs(5000)  // 会话超时时间
                .connectionTimeoutMs(15000) // 连接超时时间
                .retryPolicy(retryPolicy)
                .namespace("tortoisemq") // 隔离名称，会在zkServer创建该根目录
                .build();
        // 2.创建bootServer
        bootServer = new BrokerRemoteServer(brokerConfig);
    }

    public void start() {
        // 1.启动zkCurator
        zkCurator.start();
        // 2.启动核心server
        bootServer.start();
        // 3.注册服务到zk
        BrokerConfigUtil.registerBroker(zkCurator, brokerConfig);
        // 4.注册ShutdownHook
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    LOGGER.info("ShutdownHook invoke...");
                    BrokerController.this.shutdown();
                }
            }
        }, "ShutdownHook"));
    }

    public void shutdown() {
        // 1.关闭curator，zk因为创建的是临时节点，服务会下线
        LOGGER.info("关闭zk连接：{}", brokerConfig.getZookeeperConnect());
        zkCurator.close();
        // 2.关闭bootServer
        LOGGER.info("关闭serverBootStrap...");
        bootServer.shutdown();
    }

    public BrokerConfig getBrokerConfig() {
        return brokerConfig;
    }

    public void setBrokerConfig(BrokerConfig brokerConfig) {
        this.brokerConfig = brokerConfig;
    }

}
