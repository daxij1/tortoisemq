package cn.spoor.tortoisemq.common.util;

import cn.hutool.core.collection.CollUtil;
import cn.spoor.tortoisemq.common.config.BrokerConfig;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * @author ：spoor
 * @date ：Created in 2022/2/19 23:29
 * @description：BrokerConfigUtil
 */
public class BrokerConfigUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerConfigUtil.class);

    /**
     * 向zk中注册broker
     * */
    public static void registerBroker(CuratorFramework zkCurator, BrokerConfig brokerConfig) {
        try {
            String brokerPath = CommonConstants.BROKER_ID + CommonConstants.DIR_SPLITS + brokerConfig.getBrokerId();
            byte[] bytes = ProtostuffUtil.serializer(brokerConfig);
            zkCurator.create().creatingParentsIfNeeded().withMode(CreateMode.EPHEMERAL).forPath(brokerPath, bytes);
        } catch (Exception e) {
            LOGGER.error("zk创建节点失败，{}", e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * 从zk获取所有brokerconfig配置
     */
    public static List<BrokerConfig> requireBrokerInfo(CuratorFramework zkCurator) throws Exception {
        List<BrokerConfig> brokerConfigList = new ArrayList<>();
        // 获取zk中注册的所有brokerId
        List<String> brokerIds = zkCurator.getChildren().forPath(CommonConstants.BROKER_ID);
        if (!CollUtil.isEmpty(brokerIds)) {
            for (String brokerNode : brokerIds) {
                brokerNode = CommonConstants.BROKER_ID + CommonConstants.DIR_SPLITS + brokerNode;
                // 获取注册的brokerId的信息(序列化后的对象数组)，并反序列化为对象
                byte[] bytes = zkCurator.getData().forPath(brokerNode);
                BrokerConfig brokerInfo = ProtostuffUtil.deserializer(bytes, BrokerConfig.class);
                brokerConfigList.add(brokerInfo);
            }
        }
        return brokerConfigList;
    }

    /**
     * 从文件中读取配置
     */
    public static BrokerConfig loadConfig(String source) {
        BrokerConfig brokerConfig = new BrokerConfig();
        Properties properties = new Properties();
        InputStream is = BrokerConfigUtil.class.getClassLoader().getResourceAsStream(source);
        try {
            properties.load(is);
            brokerConfig.setBrokerId(properties.getProperty("brokerId"));
            brokerConfig.setBrokerIp(properties.getProperty("brokerIp"));
            brokerConfig.setListenPort(Integer.parseInt(properties.getProperty("listenPort")));
            brokerConfig.setServerThreadNum(Integer.parseInt(properties.getProperty("serverThreadNum")));
            brokerConfig.setZookeeperConnect(properties.getProperty("zookeeper.connect"));
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
            System.exit(-1);
        }
        return brokerConfig;
    }

}
