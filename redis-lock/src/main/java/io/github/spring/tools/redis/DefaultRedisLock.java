package io.github.spring.tools.redis;

import io.github.spring.tools.redis.capable.ILockWritable;
import io.github.spring.tools.redis.exception.UnLockFailException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.github.spring.tools.redis.RedisLockStatus.*;
import static io.github.spring.tools.redis.RedisLockStatus.TIMEOUT;

/**
 * 默认的具体Redis锁实现类
 * <p>默认的具体Redis锁实现类</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
@Data
@Slf4j
public class DefaultRedisLock implements IRedisLock, ILockWritable {

    /**
     * 状态
     */
    private RedisLockStatus status;

    /**
     * 释放状态
     */
    private RedisLockReleaseStatus releaseStatus;

    /**
     * 锁定的 key
     */
    private String key;

    /**
     * 锁定时间
     */
    private int lockSeconds;

    /**
     * 休眠最小时间
     */
    private int sleepMinMills;

    /**
     * 休眠最大时间
     */
    private int sleepMaxMills;

    /**
     * 默认自旋次数
     */
    private int spinTimes;

    /**
     * redis 锁客户端
     */
    private RedisLockClient redisLockClient;

    /**
     * 锁 值
     */
    private String uuid;

    /**
     * 回滚时抛出异常
     */
    private RuntimeException throwableException;


    /**
     * 创建一个 redis lock 对象
     * @param redisTemplate redis 实例
     * @param key 锁定的key
     * @param lockSeconds 锁定时长
     * @param sleepMinMills 休眠最小值
     * @param sleepMaxMills 休眠最大值
     * @param spinTimes 自旋次数
     * @param throwableException 是否强制抛出异常
     */
    DefaultRedisLock(RedisTemplate<String, String> redisTemplate, String key, int lockSeconds, int sleepMinMills, int sleepMaxMills, int spinTimes, RuntimeException throwableException) {
        this.key = key;
        this.lockSeconds = lockSeconds;
        this.sleepMinMills = sleepMinMills;
        this.sleepMaxMills = sleepMaxMills;
        this.spinTimes = spinTimes;
        this.status = RedisLockStatus.NEW;
        this.redisLockClient = new RedisLockClient(redisTemplate);
        this.throwableException = throwableException;
    }


    @Override
    public boolean interrupted() {
        if (isStatus(RedisLockStatus.NEW)){
            setStatus(RedisLockStatus.CANCEL);
            return true;
        }
        return false;
    }

    @Override
    public boolean needUnlock() {
        return getStatus() != RedisLockStatus.NEW;
    }

    @Override
    public RedisLockClient getLockClient() {
        return redisLockClient;
    }

    @Override
    public boolean tryLock() {
        // 获取锁
        if (doAcquire(this)){
            setStatus(RedisLockStatus.LOCKED);
            // 重置 release
            setReleaseStatus(RedisLockReleaseStatus.NEW);
            // 打印日志
            debugMessage("获取锁成功");
            return true;
        }
        debugMessage("获取锁失败");
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit);
        // 检查状态
        if (getStatus() != NEW) {
            throw new InterruptedException(String.format("%s 锁的当前状态是 %s 不能再次获取锁", getKey(), getStatus()));
        }
        // 获取成功
        try{
            if (doAcquire(time, unit)) {
                setStatus(LOCKED);
                return true;
            }else {
                // 如果 是 新的，则设置为 长时间
                if (getStatus() == NEW) {
                    setStatus(TIMEOUT);
                }
                return false;
            }
        }catch (TimeoutException ex){
            setStatus(TIMEOUT);
            return false;
        }
    }

    @Override
    public void unlock() {
        try {
            if (doRelease(this)) {
                unlocked(true);
                debugMessage("解锁成功");
            }else {
                debugMessage("解锁失败");
                unlocked(false);
            }
        } catch (UnLockFailException e) {
           unlocked(false);
            debugMessage("解锁失败");
           if (throwableException != null) {
               throw throwableException;
           }
        }
    }

    /**
     * 立即获取锁
     * @param lock 需要获取所的对象
     * @return 获取所结果
     */
    private boolean doAcquire(IRedisLock lock) {
        this.uuid = UUID.randomUUID().toString();
        return redisLockClient.setNx(lock.getKey(), this.uuid, lock.getLockSeconds());
    }


    /**
     * 支持自旋的 获取
     * @param time 时间
     * @param unit 单位
     * @return 结果
     * @throws InterruptedException 线程中断
     */
    private boolean doAcquire(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
        long timeout = System.currentTimeMillis() + unit.toMillis(time);
        int times;
        int timesCount = 0;
        do {
            times = getSpinTimes();
            // 自旋 times 次
            for (; ;) {
                checkTimeout(timeout);
                // 如果获取成功则返回成功
                if (tryLock()){
                    debugMessage(String.format(" %s次获取成功，自旋 %s 次", timesCount, getSpinTimes() - times + 1));
                    return true;
                }
                if (--times <= 0){
                    break;
                }
                timesCount ++;
                debugMessage(String.format(" %s次获取失败，自旋 %s 次", timesCount, getSpinTimes() - times));
            }
            // 随机休眠
            try {
                debugMessage(String.format(" %s次获取失败，自旋失败，开始随机休眠", timesCount));
                Thread.sleep(getSleepMills());
            } catch (Exception e) {
                return  false;
            }
        } while (true);
    }


    /**
     * 检查 超时
     * @param timeout 检查的时间
     * @throws TimeoutException 超时异常
     */
    private void checkTimeout(long timeout) throws TimeoutException {
        if (System.currentTimeMillis() >= timeout){
            throw new TimeoutException(getKey());
        }
    }

    /**
     * 释放锁
     * @param lock 需要释放锁的对象
     * @return 获取所结果
     * @throws UnLockFailException done 状态，释放失败，则抛出磁异常
     */
    private boolean doRelease(IRedisLock lock) throws UnLockFailException {
        if (StringUtils.isEmpty(uuid)) {
            throw new IllegalArgumentException(String.format("redis 共享锁 %s provider data不存在", lock.getKey()));
        }
        // 先来 lua 释放
        if (redisLockClient.releaseByLua(key, uuid)) {
            return true;
        }
        // 降级释放
        // 如果存在，key ，切value == uuid
        if (uuid.equals(redisLockClient.get(key))) {
            return redisLockClient.delete(key);
        }
        return false;
    }


    /**
     * 来一个 debug 消息
     * @param message 消息
     */
    protected void debugMessage(String message){
        log.debug(String.format("%s --> %s 锁 %s", getClass().getSimpleName(), getKey(), message));
    }
}
