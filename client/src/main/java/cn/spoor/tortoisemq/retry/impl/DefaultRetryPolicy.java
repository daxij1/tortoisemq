package cn.spoor.tortoisemq.retry.impl;

import cn.spoor.tortoisemq.retry.RetryPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/14 17:06
 * @description：DefaultRetryPolicy
 */
public class DefaultRetryPolicy implements RetryPolicy {

    private static final int MAX_RETRIES_LIMIT = 7;
    private static final int DEFAULT_MAX_SLEEP_MS = Integer.MAX_VALUE;

    private final long baseSleepTimeMs;
    private final int maxRetries;
    private final int maxSleepMs;

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultRetryPolicy.class);

    public DefaultRetryPolicy(int baseSleepTimeMs, int maxRetries) {
        this(baseSleepTimeMs, maxRetries, DEFAULT_MAX_SLEEP_MS);
    }

    public DefaultRetryPolicy(int baseSleepTimeMs, int maxRetries, int maxSleepMs) {
        this.maxRetries = maxRetries;
        this.baseSleepTimeMs = baseSleepTimeMs;
        this.maxSleepMs = maxSleepMs;
    }

    @Override
    public boolean allowRetry(int retryCount) {
        if (retryCount < maxRetries) {
            return true;
        }
        return false;
    }

    /**
     * retryCount 第一次调用为0
     * */
    @Override
    public long getSleepTimeMs(int retryCount) {
        if (retryCount < 0) {
            throw new IllegalArgumentException("重试次数必须大于0");
        }
        if (retryCount > MAX_RETRIES_LIMIT) {
            LOGGER.debug("重试次数超过{}，重试时间设为最大重试次数的时间", MAX_RETRIES_LIMIT);
            retryCount = MAX_RETRIES_LIMIT;
        }
        long sleepMs = baseSleepTimeMs * (retryCount + 1);
        if (sleepMs > maxSleepMs) {
            LOGGER.debug("重试时间超过{}，设为该时间", maxSleepMs);
            sleepMs = maxSleepMs;
        }
        return sleepMs;
    }
}
