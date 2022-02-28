package cn.spoor.tortoisemq.common.message;

/**
 * @author ：spoor
 * @date ：Created in 2022/2/19 20:10
 * @description：消息类型枚举
 */
public enum MessageType {

    // 心跳
    HEART,
    // consumer向broker注册
    CONSUMER,
    // producer向broker注册
    PRODUCER,
    // producer发送消息实体
    WRITE,
    // consumer发送拉取消息请求
    READ,
    // broker响应consumer发送消息实体
    READ_RESPONSE

}
