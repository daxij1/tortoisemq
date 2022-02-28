package cn.spoor.tortoisemq;

import cn.spoor.tortoisemq.consumer.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/16 16:21
 * @description：ConsumerTest
 */
public class ConsumerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsumerTest.class);

    public static void main(String[] args) {
        Consumer consumer = new Consumer("127.0.0.1:2181", "test");
        consumer.initialize();
        consumer.registerMessageListener(message -> {
            LOGGER.info("消费端收到消息：{}", message);
            return true;
        });
        consumer.start();
    }

}
