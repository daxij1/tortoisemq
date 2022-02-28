package cn.spoor.tortoisemq.producer;

import cn.spoor.tortoisemq.adaptor.DefaultClientAdaptor;
import cn.spoor.tortoisemq.balance.PollSelector;
import cn.spoor.tortoisemq.common.message.MessageEntity;
import cn.spoor.tortoisemq.common.message.MessageFactory;
import io.netty.channel.Channel;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/13 08:58
 * @description：Producer
 */
public class Producer extends DefaultClientAdaptor {

    private PollSelector pollSelector;

    public Producer(String zookeeperConnect) {
        super(zookeeperConnect);
    }

    public void initialize() {
        super.initialize();
        // 初始化轮询器
        pollSelector = new PollSelector();
    }

    public boolean syncSend(String topic, Object data) {
        // 构造消息实体
        MessageEntity message = MessageFactory.consMessage(topic, data);
        // 选择channel
        Channel channel = pollSelector.getCandidate(brokerChannels);
        if (channel == null) {
            return false;
        }
        // 发送消息
        try {
            channel.writeAndFlush(message).sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}
