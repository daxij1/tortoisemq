package cn.spoor.tortoisemq.common.config;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/9 10:45
 * @description：BrokerConfig，用于封装broker配置
 */
public class BrokerConfig {

    //唯一标识broker的id
    private String brokerId;

    //注册到zk的ip，客户端连接时需要用到
    private String brokerIp;

    //监听端口
    private int listenPort;

    //处理请求的线程数(workerGroup)
    private int serverThreadNum;

    //zk连接地址
    private String zookeeperConnect;

    public String getBrokerId() {
        return brokerId;
    }

    public void setBrokerId(String brokerId) {
        this.brokerId = brokerId;
    }

    public int getListenPort() {
        return listenPort;
    }

    public void setListenPort(int listenPort) {
        this.listenPort = listenPort;
    }

    public String getZookeeperConnect() {
        return zookeeperConnect;
    }

    public void setZookeeperConnect(String zookeeperConnect) {
        this.zookeeperConnect = zookeeperConnect;
    }

    public String getBrokerIp() {
        return brokerIp;
    }

    public void setBrokerIp(String brokerIp) {
        this.brokerIp = brokerIp;
    }

    public int getServerThreadNum() {
        return serverThreadNum;
    }

    public void setServerThreadNum(int serverThreadNum) {
        this.serverThreadNum = serverThreadNum;
    }
}
