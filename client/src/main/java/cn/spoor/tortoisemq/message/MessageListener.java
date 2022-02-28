package cn.spoor.tortoisemq.message;

import com.alibaba.fastjson.JSONObject;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/16 14:32
 * @description：MessageListener
 */
public interface MessageListener {

    boolean consumeMessage(JSONObject message);

}
