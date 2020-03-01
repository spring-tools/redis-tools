# redis-tools

## 介绍

基于 Spring 实现的 Redis 工具包，暂时支持：

## Lock

Redis 的 分布式锁，支持特性：

1. 重入
2. 降级
3. 回滚
4. 自旋
5. 支持 enable 引用 和 starter 的开箱即用方式。

**具体介绍** 见 [Redis-Lock](./redis-lock)

> 当前最新版本 ` lastVersion = 1.1.0`

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


##  版本更新 


* 1.0.0 发布 （2020-02-24）

完成了基本框架和分布式锁实现。
