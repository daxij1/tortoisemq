package cn.spoor.tortoisemq.test.listener;

import cn.spoor.tortoisemq.annotation.TortoiseMQMessageListener;
import cn.spoor.tortoisemq.message.MessageListener;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @author ：spoor
 * @date ：Created in 2022/2/20 19:46
 * @description：MyMessageListener
 */
@Component
@TortoiseMQMessageListener(topic = "testTopic")
public class MyMessageListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyMessageListener.class);

    @Override
    public boolean consumeMessage(JSONObject message) {
        LOGGER.info("消费端收到消息：{}", message);
        return true;
    }

}
