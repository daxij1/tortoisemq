package cn.spoor.tortoisemq.listener;

import cn.spoor.tortoisemq.client.Client;
import cn.spoor.tortoisemq.common.config.BrokerConfig;
import cn.spoor.tortoisemq.common.util.ProtostuffUtil;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.ChildData;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/15 13:51
 * @description：BrokerAwareToProducerListener
 */
public class BrokerAwareToProducerListener implements PathChildrenCacheListener {

    private final Client client;

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerAwareToProducerListener.class);

    public BrokerAwareToProducerListener(Client client) {
        this.client = client;
    }

    @Override
    public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
        PathChildrenCacheEvent.Type type = event.getType();
        ChildData data = event.getData();
        switch (type) {
            case CHILD_ADDED:
                handleNodeAdd(data);
                break;
            case CHILD_REMOVED:
                handleNodeDel(data);
                break;
            default:
                break;
        }
    }

    private void handleNodeAdd(ChildData data) {
        LOGGER.info("感知到zk新增broker节点{}，进行相关处理", data.getPath());
        BrokerConfig brokerInfo = ProtostuffUtil.deserializer(data.getData(), BrokerConfig.class);
        client.handleNodeAdd(brokerInfo);
    }

    private void handleNodeDel(ChildData data) {
        LOGGER.info("感知到zk移除broker节点{}，进行相关处理", data.getPath());
        BrokerConfig brokerInfo = ProtostuffUtil.deserializer(data.getData(), BrokerConfig.class);
        client.handleNodeDel(brokerInfo);
    }

}
