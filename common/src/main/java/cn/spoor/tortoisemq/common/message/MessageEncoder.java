package cn.spoor.tortoisemq.common.message;

import cn.spoor.tortoisemq.common.util.ProtostuffUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/13 09:32
 * @description：编码器，对象转字节数组
 */
public class MessageEncoder extends MessageToByteEncoder {

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object o, ByteBuf byteBuf) throws Exception {
        byte[] data = ProtostuffUtil.serializer(o);
        byteBuf.writeInt(data.length);
        byteBuf.writeBytes(data);
    }

}
