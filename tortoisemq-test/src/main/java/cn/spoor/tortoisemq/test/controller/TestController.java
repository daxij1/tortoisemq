package cn.spoor.tortoisemq.test.controller;

import cn.spoor.tortoisemq.producer.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author ：spoor
 * @date ：Created in 2022/2/20 19:49
 * @description：TestController
 */
@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    Producer mqProducer;

    @RequestMapping("/produce")
    public Object produce(@RequestParam("topic") String topic,@RequestParam("msg") String msg) {
        if (StringUtils.isEmpty(topic) || StringUtils.isEmpty(msg)) {
            return "topic或msg不能为空";
        }
        return mqProducer.syncSend(topic, msg);
    }

}
