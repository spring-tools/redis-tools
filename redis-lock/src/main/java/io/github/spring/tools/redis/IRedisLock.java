package io.github.spring.tools.redis;

import io.github.spring.tools.redis.capable.ILockTemplate;
import io.github.spring.tools.redis.capable.ILock;

/**
 * RedisLock的集成接口，集成了对外公布的服务接口
 * <p>RedisLock的集成接口，集成了对外公布的服务接口</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public interface IRedisLock extends ILockTemplate, ILock {

}
