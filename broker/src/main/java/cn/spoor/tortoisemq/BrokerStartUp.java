package cn.spoor.tortoisemq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ：spoor
 * @date ：Created in 2022/2/19 21:27
 * @description：BrokerStartUp
 */
public class BrokerStartUp {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrokerStartUp.class);

    //配置文件路径
    private static final String CONF_BROKER_PROPERTIES =
            System.getProperty("mqconf") == null ? "conf/broker.properties" : System.getProperty("mqconf");

    public static void main(String[] args) {
        // 创建总控器
        BrokerController brokerController = new BrokerController(CONF_BROKER_PROPERTIES);
        // 初始化
        brokerController.initialize();
        // 启动
        brokerController.start();
        LOGGER.info("BrokerStartUp start...");
    }

}
