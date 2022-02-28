package cn.spoor.tortoisemq.balance;

import cn.hutool.core.collection.CollectionUtil;
import io.netty.channel.Channel;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author ：spoor
 * @date ：Created in 2022/2/19 17:59
 * @description：轮询器，生产者每次发送消息时，从多个broker中获取其中一个
 */
public class PollSelector {

    private AtomicInteger offer = new AtomicInteger(-1);

    public Channel getCandidate(List<Channel> candidates) {
        if (CollectionUtil.isEmpty(candidates)) {
            return null;
        }
        int index = offer.incrementAndGet() % candidates.size();
        if (offer.get() == Integer.MAX_VALUE) { //不能让offer一直加下去
            offer.set(-1);
        }
        return candidates.get(index);
    }

}
