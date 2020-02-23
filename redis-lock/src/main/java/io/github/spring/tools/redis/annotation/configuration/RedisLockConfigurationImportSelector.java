package io.github.spring.tools.redis.annotation.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 共享锁 配置导入类
 * @author Fenghu.Shi
 * @version 1.0.0
 */
@Slf4j
public class RedisLockConfigurationImportSelector implements ImportSelector {

  private static final Object LOCK_OBJECT = new Object();

  private static final AtomicBoolean IS_INIT = new AtomicBoolean(false);

  @Override
  public String[] selectImports(AnnotationMetadata importingClassMetadata) {
    synchronized (LOCK_OBJECT) {
      if (IS_INIT.get()) {
        log.error("SharedLock 重复加载，本次忽略");
        return new String[]{};
      }
      // 逐个加载
      for (Map.Entry<String[], String> classes : CONFIGURATION_CLASSES.entrySet()) {
        try {
          // 如果含有则初始化对应的 configuration
          for (String clazz : classes.getKey()) {
            Class.forName(clazz, false, getClass().getClassLoader());
          }
          log.debug("共享锁 {} 客户端加载成功", classes.getValue());
          IS_INIT.set(true);
          return new String[] {classes.getValue()};
        } catch (Throwable ex) {
          log.debug("共享锁 {} 客户端装载失败，没有引用相关依赖包", classes.getValue());
        }
      }

      return new String[0];
    }
  }

  /**
   * 为了保证顺序，这里使用 linked hash map
   */
  private static final Map<String[], String> CONFIGURATION_CLASSES = new LinkedHashMap<>();

  static {
    // 注册 redis 配置
    CONFIGURATION_CLASSES.put(new String[] {"org.springframework.data.redis.core.RedisTemplate"}, "io.github.spring.tools.redis.annotation.configuration.RedisLockConfiguration");
  }
}
