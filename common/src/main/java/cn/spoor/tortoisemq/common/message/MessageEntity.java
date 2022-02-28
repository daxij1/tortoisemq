package cn.spoor.tortoisemq.common.message;

import java.util.Objects;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/13 09:01
 * @description：MessageEntity
 */
public class MessageEntity {

    private MessageType type;

    private Object data;

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageEntity that = (MessageEntity) o;
        return type == that.type &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, data);
    }

    @Override
    public String toString() {
        return "MessageEntity{" +
                "type=" + type +
                ", data=" + data +
                '}';
    }

}
