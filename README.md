## 1.why tortoisemq

- tortoisemq是一款分布式、高可用的轻量级mq产品，使用java语言开发。

- 相较于业内常用的其他mq产品，如rabbitmq、rocketmq、kafka等，tortoisemq易于部署、学习成本低，更加适合初级java程序员学习。产品设计的初衷也在于此，在开发tortoisemq的过程中，借鉴了一些rocketmq源码里的内容，化繁为简，使用者可快速上手，如果你还未开始或正准备开始阅读一些分布式中间件的源码，通过tortoisemq，我想可以为你打开一扇通往更高层次的大门。

- 从商业价值看，tortoisemq与rabbitmq等行业标杆自然不能相提并论，所以tortoisemq的命名也是略显低调，但如果你只是想异构系统，对于消息丢失、吞吐量的要求没那么严格的情况下，作为商用也未尝不可，甚至更优，毕竟逻辑简单，维护起来方便。

- tortoisemq使用zookeeper作为broker的注册中心，用于支持broker的动态注册与发现。使用netty作为组件间通信的框架支撑，对于心跳检测和客户端的断线重连有很好的支持。同时，tortoisemq提供了springboot的启动器，使用者只需在springboot项目中引入相关依赖，即可集成到项目中。

## 2.quick start

首先，确保你的pc已经安装了以下软件

```
1.64bit JDK 1.8+
2.maven 3.2.x
```

### 2.1 安装zookeeper

tortoisemq使用zookeeper作为broker管理中心，zookeeper可以单机或集群部署，这里使用单机

下载zookeeper，这里使用[zookeeper-3.5.8版本](https://archive.apache.org/dist/zookeeper/zookeeper-3.5.8/apache-zookeeper-3.5.8-bin.tar.gz)

解压，将conf目录下的zoo_sample.cfg文件重命名为zoo.cfg，并修改内容
```properties
# 监听端口
clientPort=2181
# zk数据目录，根据实际情况配置
dataDir=D:\\apache-zookeeper-3.5.8-bin\\data
# zk日志目录
dataLogDir=D:\\apache-zookeeper-3.5.8-bin\\logs
```

启动zookeeper
```sh
#windows下直接双击bin/zkServer.cmd，linux下执行以下命令
bin/zkServer.sh start conf/zoo.cfg
```

### 2.2 安装broker

下载[tortoisemq-1.0.0-release.zip](https://gitee.com/yh1996/tortoisemq/attach_files/972279/download/tortoisemq-1.0.0.zip)

解压，修改conf/broker.properties配置

```properties
# broker唯一标识
brokerId=0
# brokerIp，根据实际情况配置，下同
brokerIp=127.0.0.1
# 服务对外监听端口
listenPort=10911
# 注册中心地址，多个用逗号隔开
zookeeper.connect=127.0.0.1:2181
# 处理请求的线程数
serverThreadNum=4
```

启动broker
```sh
# 默认读取conf/broker.properties，如果需要修改，则添加-Dmqconf参数即可
java -jar -Dmqconf=conf/broker.properties tortoisemq-1.0.0.jar
```

若控制台显示BrokerStartUp start...，则启动成功

### 2.3 spring-boot使用样例

spring-boot项目中要集成tortoisemq，需要引入tortoisemq-spring-boot-starter，由于公网并没有该依赖，先手动将jar包安装到本地maven仓库

下载源码，依次进入common、client、tortoisemq-spring-boot-starter模块目录，执行maven命令安装到本地仓库

```sh
mvn clean install
```

新建springboot项目，添加依赖

```xml
<dependency>
            <groupId>cn.spoor.tortoisemq</groupId>
            <artifactId>tortoisemq-spring-boot-starter</artifactId>
            <version>1.0.0</version>
</dependency>
```

添加消费者监听
```java
import cn.spoor.tortoisemq.annotation.TortoiseMQMessageListener;
import cn.spoor.tortoisemq.message.MessageListener;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@TortoiseMQMessageListener(topic = "testTopic")
public class MyMessageListener implements MessageListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MyMessageListener.class);

    @Override
    public boolean consumeMessage(JSONObject message) {
        LOGGER.info("消费端收到消息：{}", message);
        return true;
    }

}
```

添加TestController
```java
import cn.spoor.tortoisemq.producer.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

```

yml添加配置
```yaml
tortoisemq:
  zookeeper-addr: 127.0.0.1:2181
```

启动项目，访问接口触发生产者发送消息
```
http://localhost:8080/test/produce?topic=testTopic&msg=测试消息1
```

可以看到控制台已经消费到刚刚发送的消息

![](https://gitee.com/yh1996/tortoisemq/raw/master/doc/message.png)

## 3.tortoisemq cluster

tortoisemq的集群搭建非常简单，增加broker节点，只需要保证注册地址为同一个zoookeeper集群，启动新的broker进程即可，比如在以上基础上，增加一个集群节点，只需要这样做：

复制一份broker.properties文件，命名为broker-2.properties，修改配置

```sh
# broker唯一标识
brokerId=1
# brokerIp，根据实际情况配置，下同
brokerIp=127.0.0.1
# 服务对外监听端口
listenPort=10912
# 注册中心地址，多个用逗号隔开
zookeeper.connect=127.0.0.1:2181
# 处理请求的线程数
serverThreadNum=4
```

启动broker

```sh
java -jar -Dmqconf=conf/broker-2.properties tortoisemq-1.0.0.jar
```

此时broker已添加到集群节点中，生产者发送消息时会采取轮询策略发往不同的broker

##  4.Overall Structure

TortoiseMQ架构上主要分为四部分

- Producer

  消息发布者，支持集群方式部署。Producer通过自身的负载均衡策略，选择对应的Broker节点进行消息的投递。

- Consumer

  消息消费者，支持集群方式部署。TortoiseMQ的消费端是以拉模式进行消息的消费。

- Zookeeper

  Broker的管理中心，支持Broker的动态注册与发现。Zookeeper提供了心跳检测机制，检查Broker是否保持存活，Producer，Consumer通过监听Zookeeper的数据信息从而动态感知Broker的注册与下线。

- Broker

  Broker主要负责消息的存储、投递和查询以及服务高可用保证。

通过以上介绍，相信你对tortoisemq已经有了一个初步认识，其他玩法可自行摸索，有任何问题可评论区留言......

