package cn.spoor.tortoisemq.autoconfigure;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/16 19:17
 * @description：TortoiseMQAutoConfiguration
 */
@Configuration
@Import(TortoiseMQListenerConfiguration.class)
public class TortoiseMQAutoConfiguration {

}
