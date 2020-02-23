# redis-lock

## 介绍

Redis分布式共享锁，支持多种模式

1. try/finally 原始模式
2. callback 回调模式，类似 JdbcTemplate execute
3. AOP 切面注解模式

**支持**

1. 重入
2. 降级
3. 回滚
4. 自旋
5. 支持 enable 引用 和 starter 的开箱即用方式。


## 一、快速开始

` lastVersion = 1.0.0`

### 1、MAVEN 引用

```xml
<dependency>
   <groupId>io.github.spring-tools</groupId>
   <artifactId>redis-lock-spring-boot-starter</artifactId>
   <version>${lastVersion}</version>
</dependency>

```
### 2. try/finally 使用

```java

// 来一个 try-with-resource 模式
IRedisLock lock = RedisLockBuilder.builder(LOCK_KEY).build();
try{
    Assert.assertTrue(lock.tryLock());
}finally{
    lock.unlock();
}

```

> 具体使用方式 见 [RedisLockTest](./src/test/java/io/github/spring/tools/redis/RedisLockTest.java) 


### 3. AOP 方式

> 见 [DemoService](./src/test/java/io/github/spring/tools/redis/DemoService.java) 

这里主要是基于 `io.github.spring.tools.redis.annotation.RedisLock`注解实现，具体支持的参数见 [RedisLock](./src/main/java/io/github/spring/tools/redis/annotation/RedisLock.java)

**备注**:
1. 降级回滚配置

这里也支持降级回滚等操作，主要两种方式：回退和出异常

1.  1. 配置回退方法，只支持在同一个service target中的方法，函数签名必须和执行函数一致。
使用 `RedisLock#fallbackMethod`和`RedisLock#rollbackMethod`配置

配置失败抛出异常
`RedisLock#faultThrowableException`和`RedisLock#rollbackThrowableException` 属性配置其


2. 

### 4. template 方式

```java

try {
    Assert.assertEquals("张三", RedisLockBuilder.builder(LOCK_KEY).build().execute(() -> {
        // 获取锁后的业务
        return "张三";
    }));
} catch (TimeoutLockException timeout) {
    // 这里是获取锁失败
}catch (Throwable throwable) {
    // 这里是 其他业务异常
}

```
> 具体使用方式 见 [RedisLockTemplateTest](./src/test/java/io/github/spring/tools/redis/RedisLockTemplateTest.java) 


## 二、软件架构与依赖

需要 java 8+ ，同时依赖 spring + data + redis


## 三、安装教程

安装方式，主要支持两三种自动注入方式：enabled 、spring boot starter 和 自定义 

### enabled 模式

1. maven 导入

```xml
<dependency>
   <groupId>io.github.spring-tools</groupId>
   <artifactId>redis-lock</artifactId>
   <version>${lastVersion}</version>
</dependency>

```

2. 开启引用

```java
@SpringBootApplication
@EnabledRedisLock // 开启自动注入
public class DisLockApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisLockApplication.class, args);
    }

}
```

3. 配置 spring data redis
 
这里小伙伴们自己配置吧

### spring boot starter 模式

1. maven 导入

```xml
<dependency>
   <groupId>io.github.spring-tools</groupId>
   <artifactId>redis-lock-spring-boot-starter</artifactId>
   <version>${lastVersion}</version>
</dependency>
```

2. 配置 spring data redis
 
这里小伙伴们自己配置吧



### 全局参数配置

创建一个 RedisLock 时有一些默认参数配置，如 lock 时间、自旋次数和命名空间等，可配置参数见[RedisLockBuilder](./src/main/java/io/github/spring/tools/redis/RedisLockBuilder.java)，此类配置可以记性配置，如:


```java
  
  @Before
  public void initEnv(){
    // 配置 redis lock 上下文
    RedisLockEnvironment.getBuilder().keyPrefix(KEY_PREFIX);
  }
  
```
> 这里注意，只需要定义一次即可，比如可以交由`@PostConstruct`实现。


##  版本更新 


* 1.0.0 发布 （2020-02-08）

完成了基本框架和分布式锁实现。
