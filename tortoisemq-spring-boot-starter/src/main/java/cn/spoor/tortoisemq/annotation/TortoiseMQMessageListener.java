package cn.spoor.tortoisemq.annotation;

import java.lang.annotation.*;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/16 20:04
 * @description：TortoiseMQMessageListener
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TortoiseMQMessageListener {

    String topic();

}
