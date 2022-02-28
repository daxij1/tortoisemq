package cn.spoor.tortoisemq.client;

import cn.spoor.tortoisemq.common.config.BrokerConfig;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/16 13:32
 * @description：Client
 */
public interface Client {

    /**
     * 感知到节点增加
     * */
    void handleNodeAdd(BrokerConfig brokerInfo);

    /**
     * 感知到节点减少
     * */
    void handleNodeDel(BrokerConfig brokerInfo);

}
