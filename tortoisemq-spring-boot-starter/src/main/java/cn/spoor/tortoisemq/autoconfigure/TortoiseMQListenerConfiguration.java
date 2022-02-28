package cn.spoor.tortoisemq.autoconfigure;

import cn.spoor.tortoisemq.annotation.TortoiseMQMessageListener;
import cn.spoor.tortoisemq.consumer.Consumer;
import cn.spoor.tortoisemq.message.MessageListener;
import cn.spoor.tortoisemq.producer.Producer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/16 19:23
 * @description：TortoiseMQListenerConfiguration
 */
@Configuration
public class TortoiseMQListenerConfiguration implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    @Value("${tortoisemq.zookeeper-addr}")
    private String zookeeperAddr;

    @Bean
    public Producer producer() {
        Producer producer = new Producer(zookeeperAddr);
        producer.initialize();
        return producer;
    }

    @PostConstruct
    public void post(){
        Map<String, Object> beans = this.applicationContext.getBeansWithAnnotation(TortoiseMQMessageListener.class);
        // 遍历容器中所有MessageListener，获取注解上的topic
        beans.forEach((beanName, obj) -> {
            if (obj instanceof MessageListener) {
                TortoiseMQMessageListener listerInfo = obj.getClass().getAnnotation(TortoiseMQMessageListener.class);
                Consumer consumer = new Consumer(zookeeperAddr, listerInfo.topic());
                consumer.initialize();
                consumer.registerMessageListener((MessageListener) obj);
                consumer.start();
            }
        });
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
