package io.github.spring.tools.redis;

import io.github.spring.tools.redis.annotation.EnabledRedisLock;
import io.github.spring.tools.redis.annotation.configuration.RedisLockConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

/**
 * 自动装载
 * @author Fenghu.Shi
 * @version 1.0
 */
@Configuration
@ConditionalOnMissingBean(RedisLockConfiguration.class)
@EnabledRedisLock
public class RedisLockAutoConfiguration {

}
