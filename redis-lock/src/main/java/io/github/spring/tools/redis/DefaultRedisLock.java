package io.github.spring.tools.redis;

import io.github.spring.tools.redis.capable.ILockWritable;
import io.github.spring.tools.redis.exception.UnLockFailException;
import lombok.Data;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 默认的具体Redis锁实现类
 * <p>默认的具体Redis锁实现类</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
@Data
public class DefaultRedisLock implements RedisLock, ILockWritable {

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
    public boolean tryLock() {
        // 获取锁
        if (doAcquire(this)){
            setStatus(RedisLockStatus.LOCKED);
            return true;
        }
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit);
        long endTime = System.currentTimeMillis() + unit.toMillis(time);
        do{
            // 检查是否取消
            if (isStatus(RedisLockStatus.CANCEL)) {
                throw new InterruptedException();
            }
            // 如果获取成功
           if (tryLock()) {
                return true;
           }
           // sleep 一下
            Thread.sleep(getSleepMills());
        }while(System.currentTimeMillis() < endTime);
        return false;
    }

    @Override
    public void unlock() {
        try {
            if (doRelease(this)) {
                unlocked(true);
            }else {
                unlocked(false);
            }
        } catch (UnLockFailException e) {
           unlocked(false);
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
    private boolean doAcquire(RedisLock lock) {
        this.uuid = UUID.randomUUID().toString();
        return redisLockClient.setNX(lock.getKey(), this.uuid, lock.getLockSeconds());
    }

    /**
     * 释放锁
     * @param lock 需要释放锁的对象
     * @return 获取所结果
     * @throws UnLockFailException done 状态，释放失败，则抛出磁异常
     */
    private boolean doRelease(RedisLock lock) throws UnLockFailException {
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
}
