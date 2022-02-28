package cn.spoor.tortoisemq;

import cn.spoor.tortoisemq.producer.Producer;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/13 09:48
 * @description：Test
 */
public class ProducerTest {

    public static void main(String[] args) {
        Producer producer = new Producer("127.0.0.1:2181");
        producer.initialize();
        AtomicInteger i = new AtomicInteger();
        mockProduce(producer, i);
    }

    /**
     * 模拟消息不断发送
     * */
    private static void mockProduce(Producer producer, AtomicInteger i) {
        Executors.newSingleThreadScheduledExecutor().schedule(() -> {
            try {
                producer.syncSend("test", "测试消息" + i.incrementAndGet());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                mockProduce(producer, i);
            }
        }, 5000, TimeUnit.MILLISECONDS);
    }

}
