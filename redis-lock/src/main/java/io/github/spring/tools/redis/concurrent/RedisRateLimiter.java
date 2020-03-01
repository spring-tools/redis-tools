package io.github.spring.tools.redis.concurrent;

import io.github.spring.tools.redis.IRedisLock;
import io.github.spring.tools.redis.RedisLockBuilder;
import io.github.spring.tools.redis.RedisLockClient;
import io.github.spring.tools.redis.capable.ILockWritable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.max;

/**
 * 基于 Redis 的分布式令牌桶，暂时精确度到 毫秒，即一秒 1000次
 * <p>基于 Redis 的分布式令牌桶，参照 <a href="https://github.com/google/guava"></a>Guava RateLimiter</a></p>
 * <p>
 *
 *     主要属性：
 *      permitsPerSecond 每秒产生令牌数
 *      maxBurstSeconds 最大存储的令牌秒数
 *      nextFreeTicketMills 下一次释放令牌的时间，毫秒
 *      storedPermits 当前存储的令牌数量
 *      maxPermits 最大存储的令牌数量
 * </p>
 * @author Fenghu.Shi
 * @version 1.1.0
 */
@Slf4j
public class RedisRateLimiter {


    /**
     * 每秒产生的令牌数
     */
    private double permitsPerSecond;

    /**
     * 最大存储的令牌数量
     */
    private double maxPermits;

    /**
     * 默认第一次初始化的存储令牌数量
     */
    private double initStoredPermits;

    /**
     * 限速器数据
     */
    private RateLimiterData limiterData;

    /**
     * 分布式锁的 key
     */
    private String key;

    private RedisLockClient redisLockClient;

    /**
     * 设置
     * @param permitsPerSecond 每秒产生的数量
     * @param maxPermits 最大存储的令牌数量
     * @param initStoredPermits 初始化第一次的存储令牌数量
     * @param key 锁定的key
     */
    private RedisRateLimiter(double permitsPerSecond, double maxPermits, double initStoredPermits, String key){
        this.permitsPerSecond = permitsPerSecond;
        this.maxPermits = maxPermits;
        this.initStoredPermits = initStoredPermits;
        this.key = key;
    }


    /**
     * 检查参数
     * @param b 要检查的值
     * @param errorMessageTemplate 抛出的异常内容
     */
    private static void checkArgument(boolean b, String errorMessageTemplate) {
        if (!b) {
            throw new IllegalArgumentException(errorMessageTemplate);
        }
    }

    /**
     * 尝试获取锁，不等待
     * @param permits 获取的数量
     * @return 结果
     */
    public boolean tryAcquire(int permits) {
        try {
            return tryAcquire(permits, 0, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 尝试获取当前可用的数量
     * @return 令牌数量
     */
    public long tryGetAllPermits(){
        try {
            try(IRedisLock redisLock = RedisLockBuilder.builder(key).lockSeconds(getLockSeconds(0)).build()){
                if (redisLock.tryLock()) {
                    long nowMills = currentRedisTime();
                    long usedMills = System.currentTimeMillis();
                    restore(nowMills);
                    usedMills = System.currentTimeMillis() - usedMills;
                    long getPerimts = Double.valueOf(limiterData.getStoredPermits()).intValue();
                    // 检查是否有数量，如果有数量，则消费一下
                    if (getPerimts > 0) {
                        syncToRedis(getPerimts, nowMills + usedMills);
                    }
                    return getPerimts;
                }
            }
        } catch (Exception e) {}
        return 0;
    }

    /**
     * 尝试获取 permits 个令牌
     * @param permits 令牌数量
     * @param timeout 超时时间
     * @param unit 单位
     * @return 获取结果
     */
    public boolean tryAcquire(int permits, int timeout, TimeUnit unit) throws Exception {
        Objects.requireNonNull(key);
        Objects.requireNonNull(unit);
        timeout = max(0, timeout);
        long timeoutMills = max(unit.toMillis(timeout), 0);
        checkPermits(permits);
        long millsToWait = 0;
        long startLockMills = System.currentTimeMillis();
        try(IRedisLock redisLock = RedisLockBuilder.builder(key).lockSeconds(getLockSeconds(timeoutMills)).build()){
            // 锁定资源
            if (timeout > 0 ? redisLock.tryLock(timeout, unit) : redisLock.tryLock()) {
                long nowMills = currentRedisTime();
                restore(nowMills);
                long lockUsedMills = System.currentTimeMillis() - startLockMills;
                // 检查直到超时时间点，是否可满足发放令牌时间
                if (!canAcquire(permits, nowMills, timeoutMills) && timeout == 0) {
                    debug(String.format("获取失败，存%s,需%s", limiterData.getStoredPermits(), permits));
                    return false;
                } else {
                    millsToWait = getWaitLength(permits, nowMills);
                    debug(String.format("当前库存不足，需等待 %s 毫秒", millsToWait));
                }
                // 检查等待时间是否在 timeout 时间以内
                if (millsToWait > 0 && (unit.toMillis(timeout) - lockUsedMills - millsToWait) < 0 ){
                    debug(String.format("等待时间内不足以获取，等待时间 %s 毫秒，获取此 %s 令牌数量需 %s 毫秒", unit.toMillis(timeout), permits, millsToWait + lockUsedMills));
                    return false;
                }
                // 如果需要等待，则等待
                if (millsToWait > 0) {
                    debug(String.format("等待 %s 毫秒可取得令牌,%s,%s,%s", millsToWait, unit.toMillis(timeout), limiterData.getNextFreeTicketMill(), lockUsedMills));
                    Thread.sleep(millsToWait);
                    nowMills +=  millsToWait + lockUsedMills;
                }
                // 同步到 redis
                syncToRedis(permits, nowMills);
                debug(String.format(" tostring is %s", limiterData.toString()));
                return true;
            }else {
                return false;
            }
        }
    }

    /**
     * 获取锁定的时间
     * @param timeoutMills 超时毫秒
     * @return 锁定key 的秒数
     */
    private int getLockSeconds(long timeoutMills){
        return timeoutMills == 0 ? ILockWritable.DEFAULT_LOCK_SECONDS : (int) Math.max(TimeUnit.MILLISECONDS.toSeconds(timeoutMills) * 10 , ILockWritable.DEFAULT_LOCK_SECONDS);
    }

    /**
     * 是否可以获取
     * @param permits 需要的数量
     * @param nowMills 当前的时间点
     * @param timeoutMills 超时时间点
     * @return 是否能获取
     */
    private boolean canAcquire(int permits, long nowMills, long timeoutMills) {
        debug(String.format("尝试直接获取存储的令牌[存%s,需:%s]", limiterData.getStoredPermits(), permits));
        return permits <= limiterData.getStoredPermits();
    }

    /**
     * 获取当前 redis 时间
     * @return 当期那时间，毫秒
     */
    private long currentRedisTime(){
        return redisLockClient.queryRedisNow();
    }

    /**
     * 检查 令牌数量
     * @param permits 检查的数量
     */
    private static void checkPermits( int permits) {
        checkArgument(permits > 0, String.format("Requested permits (%s) must be positive", permits));
    }

    /**
     * 同步到 redis 中
     * @param permites 需要的数量
     * @param now 时间
     */
    private void syncToRedis(long permites, long now){
        limiterData.acquire(permites, now);
        redisLockClient.set(newKey(), limiterData.toArrayString(),true);
    }

    /**
     * 生成 key
     * @return redis key
     */
    private String newKey(){
        return String.format("%s%s", DATA_KEY_PREFIX, this.key);
    }

    /**
     * 获取等待时间
     * @param permits 要获取的令牌数
     * @param now 当前时间点
     * @return 要等待的时长，微妙
     */
    private long getWaitLength(int permits, long now){
        // 检查存储的数量是否够用，如果够了则不用等待
        if (limiterData.getStoredPermits() >= permits) {
            return 0L;
        }
        // 计算下一次 时间
        return TimeUnit.SECONDS.toMillis(Double.valueOf((permits - getStoredPermits(now)) / limiterData.getPermitsPerSecond()).longValue());
    }

    /**
     * 获取当前存储的令牌
     * @param now 当前时间
     * @return 令牌数量
     */
    private double getStoredPermits(long now){
        return limiterData.getStoredPermits();
    }

    /**
     * 从 redis 还原数据，如果第一次，则新建一个
     * @param now 当前时间
     */
    private void restore(long now){
        long startRestoreMills = System.currentTimeMillis();
        String value = redisLockClient.get(newKey());
        now +=  System.currentTimeMillis() - startRestoreMills;
        // 如果存在则根据 value 还原
        if (!StringUtils.isEmpty(value)) {
            debug(String.format("从 redis 恢复 数据, %s", value));
            limiterData = RateLimiterData.of(value, permitsPerSecond, maxPermits, key);
        }else {
            // 不存在则new 一个
            limiterData = RateLimiterData.of(permitsPerSecond, maxPermits, initStoredPermits, now);
            debug(String.format("第一次初始化限流器, %s", limiterData.toString()));
        }
        // 计算一把
        limiterData.resync(now);
    }

    /**
     * 创建一个实例，默认最大存储1秒的数量，存储1秒的令牌
     * @param key key
     * @param permitsPerSecond 每秒产生数量
     * @return 对象
     */
    public static RedisRateLimiter create(String key, double permitsPerSecond) {
        return create(key, permitsPerSecond, 1.0, 1.0);
    }

    /**
     * 设置一个 maxBurstSeconds 的限流器，initBurstSeconds = maxBurstSeconds
     * @param key key
     * @param permitsPerSecond 每秒产生数量
     * @param maxBurstSeconds 最大存储的秒数
     * @return 对象
     */
    public static RedisRateLimiter create(String key, double permitsPerSecond, double maxBurstSeconds){
        return create(key, permitsPerSecond, maxBurstSeconds, maxBurstSeconds);
    }

    /**
     * 创建一个实例
     * @param key key
     * @param permitsPerSecond 每秒产生数量
     * @param maxBurstSeconds 最大存储描述
     * @param initBurstSeconds 第一次初始化，默认含有的数量
     * @return 对象
     */
    public static RedisRateLimiter create(String key, double permitsPerSecond, double maxBurstSeconds, double initBurstSeconds){
        Assert.isTrue(maxBurstSeconds > 0, "maxBurstSeconds 必须大于0");
        RedisRateLimiter limiter = new RedisRateLimiter(permitsPerSecond, maxBurstSeconds * permitsPerSecond, initBurstSeconds * permitsPerSecond, key);
        // 设置 redis lock client
        limiter.redisLockClient = ((ILockWritable) RedisLockBuilder.builder(DATA_KEY_PREFIX).build()).getLockClient();
        return limiter;
    }

    /**
     * 消息
     * @param message 消息内容
     */
    private void debug(String message){
        log.debug("RateLimiter:key={} {}", key, message);
    }

    /**
     * data 的 key  前缀
     */
    private static final String DATA_KEY_PREFIX = "RedisRateLimiterKey:";
}
