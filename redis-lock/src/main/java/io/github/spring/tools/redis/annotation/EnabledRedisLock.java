package io.github.spring.tools.redis.annotation;


import io.github.spring.tools.redis.annotation.configuration.RedisLockConfigurationImportSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 开启 共享锁
 * @author Fenghu.Shi
 * @version 1.0
 **/
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(RedisLockConfigurationImportSelector.class)
public @interface EnabledRedisLock {}
