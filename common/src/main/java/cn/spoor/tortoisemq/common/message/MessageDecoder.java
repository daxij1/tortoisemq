package cn.spoor.tortoisemq.common.message;

import cn.spoor.tortoisemq.common.util.ProtostuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/13 09:27
 * @description：解码器
 */
public class MessageDecoder extends ByteToMessageDecoder {

    // Message的class类型
    private final Class<?> genericClass;

    public MessageDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf buf, List<Object> list) throws Exception {
        // int占四个字节
        if (buf.readableBytes() < 4) {
            return;
        }
        buf.markReaderIndex();
        int dataLength = buf.readInt();
        // 如果可读长度小于dataLength，重置读取位置
        if (buf.readableBytes() < dataLength) {
            buf.resetReaderIndex();
            return;
        }
        byte[] data = new byte[dataLength];
        buf.readBytes(data);
        list.add(ProtostuffUtil.deserializer(data, genericClass));
    }

}
