package io.github.spring.tools.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands.SetOption;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.types.Expiration;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Objects;

/**
 * 基于spring 的共享锁客户端
 * @author Fenghu.Shi
 * @version 1.0
 */
@Slf4j
public class RedisLockClient {

  /**
   * 解锁的lua脚本
   */
  private static final String RELEASE_LUA;

  /**
   * 获取当前时间的LUA命令
   */
  private static final String TIME_LUA = "return redis.call('TIME')";

  static {
    StringBuilder sb = new StringBuilder();
    sb.append("if redis.call(\"get\",KEYS[1]) == ARGV[1] ");
    sb.append("then ");
    sb.append("    return redis.call(\"del\",KEYS[1]) ");
    sb.append("else ");
    sb.append("    return 0 ");
    sb.append("end ");
    RELEASE_LUA = sb.toString();
  }

  protected final RedisTemplate<String, String> redisTemplate;

  /**
   * 创建一个 redis lock client
   * @param redisTemplate
   */
  public RedisLockClient(RedisTemplate<String, String> redisTemplate){
    this.redisTemplate = redisTemplate;
  }

  /**
   * 获取 get 对应的数据
   * https://redis.io/commands/get
   * @param key 锁的key
   * @return
   */
  public String get(String key) {
    return redisTemplate.opsForValue().get(key);
  }

  /**
   * 设置 数据
   * https://redis.io/commands/set
   * @param key key
   * @param value 值
   * @param isPersist 是否需要持久化
   */
  public void set(String key, String value, boolean isPersist){
    redisTemplate.opsForValue().set(key, value);
    if (isPersist){
      redisTemplate.persist(key);
    }
  }

  /**
   * 调用 set nx 接口
   * https://redis.io/commands/set
   * @param key 锁 key
   * @param value 锁的值，一般是UUID
   * @param lockSeconds 锁定时长，单位秒
   * @return 设置结果
   */
  public boolean setNx(String key, String value, int lockSeconds) {
    return redisTemplate.execute((RedisConnection connection) -> {
      try {
       if (connection.set(RedislockUtils.stringToBytes(key), RedislockUtils.stringToBytes(value), Expiration.seconds(lockSeconds), SetOption.SET_IF_ABSENT)) {
         log.debug("spring data redis -> {} 获取锁{}数据成功", key, value);
         return true;
       }
      } catch (Exception e) {
        log.error("spring data redis -> {} 锁获取超时", key, e);
      }
      return false;
    });
  }

  /**
   * 通过Lua脚本释放锁 https://redis.io/commands/eval
   *
   * @param key 锁 key * @param value 锁的值，一般是UUID
   * @param value 锁值
   * @return
   */
  public boolean releaseByLua(String key, String value) {
    return redisTemplate.execute((RedisConnection connection) -> {
      try {
        if (connection.eval(RedislockUtils.stringToBytes(RELEASE_LUA), ReturnType.BOOLEAN, 1, RedislockUtils.stringToBytes(key), RedislockUtils.stringToBytes(value))) {
          log.debug("spring data redis -> {} 释放锁成功", key);
          return true;
        }
      } catch (Exception e) {
        log.error("spring data redis -> {} 释放锁超时", key, e);
      }
      return false;
    });
  }

  /**
   * 执行 script
   * @param script 要执行的 script
   * @return 值
   */
  public <T> List<T> execScriptList(String script){
    return redisTemplate.execute((RedisConnection connection) -> {
      return connection.eval(RedislockUtils.stringToBytes(script), ReturnType.MULTI, 0);
    });
  }

  /**
   * 获取当前 redis 时间
   * @return 结果
   */
  public long queryRedisNow(){
    List<byte[]> times = execScriptList(TIME_LUA);
    StringBuilder strs = new StringBuilder();
    Objects.requireNonNull(times);
    Assert.isTrue(times.size()  == 2, "从 redis 获取 当前服务器时间失败");
    Assert.isTrue(times.get(1).length > 3, "从 redis 获取 当前服务器时间失败");
    // 取秒
    strs.append(new String(times.get(0)));
    strs.append(new String(times.get(1)).substring(0, 3));
    System.out.println("now is " + new String(times.get(0)) + new String(times.get(1)));
    return Long.valueOf(strs.toString());
  }

  /**
   * 删除 key
   * https://redis.io/commands/del
   * @param key 锁 key
   * @return
   */
  public boolean delete(String key) {
    return redisTemplate.delete(key);
  }


  /**
   * 获取 redis  template
   * @return redis template 对象
   */
  public RedisTemplate<String, String> getRedisTemplate(){
    return redisTemplate;
  }

}
