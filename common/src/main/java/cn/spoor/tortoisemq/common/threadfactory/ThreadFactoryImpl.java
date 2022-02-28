package cn.spoor.tortoisemq.common.threadfactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author ：spoor
 * @date ：Created in 2022/2/19 21:40
 * @description：线程工厂-这里主要修改线程创建时的命名规则
 */
public class ThreadFactoryImpl implements ThreadFactory {

    private final AtomicLong threadIndex = new AtomicLong(0);
    // 线程名前缀
    private final String threadNamePrefix;
    // 是否守护线程，默认不是
    private final boolean daemon;

    public ThreadFactoryImpl(final String threadNamePrefix) {
        this(threadNamePrefix, false);
    }

    public ThreadFactoryImpl(final String threadNamePrefix, boolean daemon) {
        this.threadNamePrefix = threadNamePrefix;
        this.daemon = daemon;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, threadNamePrefix + "-" + this.threadIndex.incrementAndGet());
        thread.setDaemon(daemon);
        return thread;
    }

}

