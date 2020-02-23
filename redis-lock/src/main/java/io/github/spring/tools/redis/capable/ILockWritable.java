package io.github.spring.tools.redis.capable;

import io.github.spring.tools.redis.RedisLockReleaseStatus;
import io.github.spring.tools.redis.RedisLockStatus;

/**
 * 定义锁属性变更接口
 * <p>定义锁属性变更接口</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public interface ILockWritable {

    /**
     * 设置 状态
     * @param status 要设置的状态
     */
    void setStatus(RedisLockStatus status);

    /**
     * 设置 key
     * @param key 要设置的key
     */
    void setKey(String key);

    /**
     * 当前状态
     * @param status 要检查的状态
     * @return 是否一致
     */
    default boolean isStatus(RedisLockStatus status) {
        return getStatus() == status;
    }

    /**
     * 是否需要解锁
     * @return 是否需要解锁
     */
    boolean needUnlock();


    /**
     * 设置 release 状态
     * @param releaseStatus 释放状态
     */
    void setReleaseStatus(RedisLockReleaseStatus releaseStatus);

    /**
     * 设置 锁定时间，单位秒
     * @param lockSeconds 要设置的锁定时长
     */
    void setLockSeconds(int lockSeconds);

    /**
     * 获取lock的时间
     * @return 锁定时长
     */
    int getLockSeconds();

    /**
     * 设置 休眠最小时间，单位毫秒
     * @param sleepMinMills 最小休眠时间
     */
    void setSleepMinMills(int sleepMinMills);

    /**
     * 获取最小休眠时间，毫秒
     * @return 当前最小休眠时间
     */
    int getSleepMinMills();

    /**
     * 设置 休眠最大时间，单位毫秒
     * @param sleepMaxMills
     */
    void setSleepMaxMills(int sleepMaxMills);

    /**
     * 获取最大休眠时间
     * @return
     */
    int getSleepMaxMills();

    /**
     * 设置 自旋次数
     * @param spinTimes
     */
    void setSpinTimes(int spinTimes);

    /**
     * 获取自旋次数
     * @return
     */
    int getSpinTimes();

    /**
     * 获取当前状态
     * @return
     */
    RedisLockStatus getStatus();

    /**
     * 获取当前休眠的时间
     * @return
     */
    default int getSleepMills(){
        return getSleepMinMills() + (int) ((getSleepMaxMills() - getSleepMinMills()) * Math.random());
    }

    /**
     * 当前状态在给定的状态列表中
     * @param statuses 检查的状态列表
     * @return 结果
     */
    default boolean inStates(RedisLockStatus...statuses){
        // 为空 直接返回 false
        if (statuses == null) {
            return false;
        }
        for (RedisLockStatus status : statuses){
            if (status == getStatus()){
                return true;
            }
        }
        return false;
    }

    /**
     * 成功并设置状态
     * @param release 是否释放成功
     */
    default void unlocked(boolean release) {
        setReleaseStatus(release ? RedisLockReleaseStatus.SUCCESS : RedisLockReleaseStatus.FAIL);
    }


    /**
     * 默认自旋数量
     */
    public static final int DEFAULT_SPIN_TIME = 3;

    /**
     * 最小休眠时间
     */
    public static final int DEFAULT_SLEEP_MIN_MILLS = 200;

    /**
     * 最大休眠时间
     */
    public static final int DEFAULT_SLEEP_MAX_MILLS = 500;

    /**
     * 默认锁定时长，单位秒
     */
    public static final int DEFAULT_LOCK_SECONDS = 10;
}
