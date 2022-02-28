package cn.spoor.tortoisemq.retry;

/**
 * @author ：spoor
 * @date ：Created in 2021/9/14 17:05
 * @description：RetryPolicy
 */
public interface RetryPolicy {

    /**
     * Called when an operation has failed for some reason. This method should return
     * true to make another attempt.
     *
     * @param retryCount the number of times retried so far (0 the first time)
     * @return true/false
     */
    boolean allowRetry(int retryCount);

    /**
     * get sleep time in ms of current retry count.
     *
     * @param retryCount current retry count
     * @return the time to sleep
     */
    long getSleepTimeMs(int retryCount);

}
