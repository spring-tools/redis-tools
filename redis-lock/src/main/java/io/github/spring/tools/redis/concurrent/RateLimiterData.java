package io.github.spring.tools.redis.concurrent;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 限速器数据
 * <p>限速器数据源</p>
 *
 * @author Fenghu.Shi
 * @version 1.1.0
 */
@Getter
@Slf4j
public class RateLimiterData implements Serializable {

    /**
     * 每秒产生的令牌数
     */
    private double permitsPerSecond;

    /**
     * 下一次触发令牌发送的时间
     */
    private long nextFreeTicketMill;

    /**
     * 当前存储的令牌数量
     */
    private double storedPermits;

    /**
     * 最大存储的令牌数量
     */
    private double maxPermits;

    private RateLimiterData(){}


    /**
     * 转换成  array  的 字符串
     * @return 转换成 array string
     */
    public String toArrayString(){
        return String.format("%s,%s,%s,%s", permitsPerSecond, nextFreeTicketMill, storedPermits, maxPermits);
    }


    /**
     * 通过 array string 转换
     * @param arrayString 字符串
     * @param permitsPerSecond 每秒生成数量
     * @param maxPermits 最大存储令牌数
     * @param key key
     * @return 数据
     */
    public static RateLimiterData of(String arrayString, double permitsPerSecond, double maxPermits, String key){
        Objects.requireNonNull(arrayString);
        RateLimiterData data = new RateLimiterData();
        // 解析
        String[] strs = arrayString.split(",");
        Assert.isTrue(strs.length == 4, String.format("需要解析的rate limit 字符串[%s] 格式错误", arrayString));
        data.permitsPerSecond = Double.valueOf(strs[0]);
        data.nextFreeTicketMill = Long.valueOf(strs[1]);
        data.storedPermits = Double.valueOf(strs[2]);
        data.maxPermits = Double.valueOf(strs[3]);
        StringBuilder overriedMessage = new StringBuilder();
        if (data.permitsPerSecond != permitsPerSecond) {
            overriedMessage.append(String.format("permitsPerSecond:[%s,%s],", permitsPerSecond, data.permitsPerSecond));
            data.permitsPerSecond = permitsPerSecond;
        }
        if (data.maxPermits != maxPermits){
            overriedMessage.append(String.format("maxPermits:[%s:%s]", maxPermits, data.maxPermits));
            data.maxPermits = maxPermits;
        }
        if (overriedMessage.length() > 0){
            log.error(String.format("RateLimiter[%s]配置参数被重置[新:旧], %s", key, overriedMessage.toString()));
        }
        return data;
    }


    /**
     * 第一次初始化
     * @param permitsPerSecond 每秒生成数量
     * @param maxPermits 最大存储令牌数
     * @param initStoredPermits 初始化的令牌数量
     * @param now 当前时间
     * @return 数据对象
     */
    public static RateLimiterData of(double permitsPerSecond,  double maxPermits, double initStoredPermits, long now){
        RateLimiterData data = new RateLimiterData();
        data.permitsPerSecond = permitsPerSecond;
        data.nextFreeTicketMill = now;
        data.storedPermits = initStoredPermits;
        data.maxPermits = maxPermits;
        data.resync(now);
        return data;
    }

    /**
     * 获取 数量
     * @param storedPermits 设置最终存储的数量
     * @param nextFreeTicketMill 下一此的时间
     */
    public void acquire(double storedPermits, long nextFreeTicketMill){
        this.storedPermits = Math.max(this.storedPermits - storedPermits, 0);
        this.nextFreeTicketMill = nextFreeTicketMill;
    }

    /**
     * 重新 计算一把，主要计算 storedPermits 和 nextFreeTicketMicros
     * @param now 当前时间
     */
    public void resync(long now){
        // 如果下一次释放时间在当前之前之后，则忽略
        if (now - this.nextFreeTicketMill < 0){
            return;
        }
        double storedPermits = this.storedPermits + TimeUnit.MILLISECONDS.toSeconds(now - this.nextFreeTicketMill) * this.getPermitsPerSecond();
        this.storedPermits = Math.min(storedPermits, maxPermits);
    }


    @Override
    public String toString() {
        return "RateLimiterData{" +
                "permitsPerSecond=" + permitsPerSecond +
                ", nextFreeTicketMill=" + nextFreeTicketMill +
                ", storedPermits=" + storedPermits +
                ", maxPermits=" + maxPermits +
                '}';
    }
}
