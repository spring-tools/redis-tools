# shared-lock

## 介绍

分布式共享锁，支持多种模式

1. try/finally 原始模式
2. callback 回调模式，类似 JdbcTemplate execute
3. AOP 切面注解模式

**支持**

1. 重入
2. 降级


支持 enable 引用 和 starter 的开箱即用方式。


## 一、快速开始

### 1、MAVEN 引用

```xml
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>spring-boot-starter-shared-lock</artifactId>
   <version>${lastVersion}</version>
</dependency>
<!-- redis -->
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>lock-redis</artifactId>
   <version>${lastVersion}</version>
</dependency>

<!-- zookeeper -->
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>lock-zookeeper</artifactId>
   <version>${lastVersion}</version>
</dependency>
```
### 2. try/finally 使用

```java

    // 来一个 try-with-resource 模式
    try(ISharedLock lock = SharedLockBuilder.builder(LOCK_KEY).build()) {
      if (lock.tryLock(5, TimeUnit.SECONDS)) {
        System.out.println("执行成功");
        return Flux.just("OK");
      } else {
        return Flux.error(new Throwable("失败了"));
      }
    }


```

### 3. AOP 方式使用

```java

package org.shared.lock.demo;

import lombok.extern.slf4j.Slf4j;
import net.madtiger.lock.SharedLock;
import org.springframework.stereotype.Service;

/**
 * 样例
 * @author Fenghu.Shi
 * @version 1.0
 */
@Service
@Slf4j
public class DemoService {


  /**
   * 执行入口
   * @param abc
   * @return
   */
  @SharedLock(key ="demo-test-#{abc}", fallbackMethod = "faultBack", rollbackMethod = "rollback")
  public String testAopLock(String abc){
      log.error("我获取到锁了");
      return "这是我得知";
  }

  /**
   * 失败降级方法
   * @param bac
   * @return
   */
  public String faultBack(String bac){
    log.error("我被降级了");
    return "降级哈哈";
  }

  /**
   * 回滚方法
   * @param abc
   */
  public String rollback(String abc){
    System.out.println("回滚了");
    return "我被回滚了";
  }
}


```



## 二、软件架构

需要 java 8+ ，同时依赖 spring

1. redis 
依赖 spring data reids

2. zookeeper
依赖 Curator Framework

### 名词说明

英文名称|中文名称|描述
:---:|:--:|:---:
ISharedLock|锁持有对象|锁的持有者和操作者
Provider|服务提供者|最终底层实现的底层服务，比如 ZooKeeper，Redis，都会有对应的Provider
Builder|构造模式|用于快速链式构造对象，如 SharedLockBuilder
Environment|环境|用于定义SharedLock环境信息，如 SharedLockEnvironment
Decorator|装饰者|SharedLock 内部除了基本功能意外的特性都是通过 Decorator 实现的，如 自旋锁（redis 支持），可重入锁


## 三、安装教程

安装方式，主要支持两三种自动注入方式：enabled 、spring boot starter 和 自定义 

### enabled 模式

1. maven 导入

```xml
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>lock-annotation</artifactId>
   <version>${lastVersion}</version>
</dependency>
<!-- redis -->
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>lock-redis</artifactId>
   <version>${lastVersion}</version>
</dependency>
<!-- zookeeper -->
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>lock-zookeeper</artifactId>
   <version>${lastVersion}</version>
</dependency>

```

2. 开启引用

```java
@SpringBootApplication
@EnabledSharedLock // 开启自动注入
public class DisLockApplication {

    public static void main(String[] args) {
        SpringApplication.run(DisLockApplication.class, args);
    }

}
```

3. 配置 spring data redis / curator framework
 
这里小伙伴们自己配置吧

### spring boot starter 模式

1. maven 导入

```xml
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>spring-boot-starter-shared-lock</artifactId>
   <version>${lastVersion}</version>
</dependency>
<!-- redis -->
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>lock-redis</artifactId>
   <version>${lastVersion}</version>
</dependency>
<!-- zookeeper -->
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>lock-zookeeper</artifactId>
   <version>${lastVersion}</version>
</dependency>
```

2. 配置 spring data redis
 
这里小伙伴们自己配置吧


### ~~~自定义 (该方式不推荐使用)~~~

此方式稍微麻烦点，不建议大家使用哈

### 1. maven 导入 

```xml
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>lock-annotation</artifactId>
   <version>${lastVersion}</version>
</dependency>
<!-- redis -->
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>lock-redis</artifactId>
   <version>${lastVersion}</version>
</dependency>
<!-- zookeeper -->
<dependency>
   <groupId>net.madtiger.shared.lock</groupId>
   <artifactId>lock-zookeeper</artifactId>
   <version>${lastVersion}</version>
</dependency>
```

### 2. 初始化 service bean

SharedLock 组件使用主要涉及三个类

1. SpringRedisLockClient 
用于 操作 redis 的客户端 ，底层是通过 spring-data-redis 的 RedisTemplate 实现的

2. RedisLockService / ZookeeperLockService
分布式锁的Redis/ZK具体实现类


3. SharedLockInterceptor 
用于支持 AOP 模式的拦截器，这里是可选的，如果不需要支持 拦截器则不用实例化


**样例**:
```java
public class CustomizeSharedLockConfiguration {

  /**
   * 配置 redis 共享锁服务提供者
   * @param redisTemplate
   * @return
   */
  @PostConstruct
  public void defaultSharedLock(RedisTemplate redisTemplate){
    ISharedLockProvider provider = new RedisLockProvider(new RedisLockClient(redisTemplate));
    // 这里 addDecorator classes 可以添加一些支持的特性具体见特性介绍
    SharedLockEnvironment.getInstance().addDecoratorClasses(defaultDecorators()).setDefaultProvder(provider);
  }
  /**
  * zk 共享锁
  */
  @PostConstruct
  public void defaultSharedLock(CuratorFramework zookeeper){
    ISharedLockProvider provider = new ZookeeperLockProvider(new CuratorLockClient(zookeeper));
    // 这里 addDecorator classes 可以添加一些支持的特性具体见特性介绍
    // 同时可以设置全局的 lockSeconds 、 provider（如果有多个）和  zk 的锁父节点
    SharedLockEnvironment.getInstance().setConfigurer(ZookeeperLockProvider.class, ZookeeperConfigurer.builder().namespace("/lock-namespace").build()).addDecoratorClasses(defaultDecorators()).setDefaultProvder(provider);
  }

 /**
   * 创建 共享锁 拦截器
   * @param context
   * @return
   */
  @Bean
  public SharedLockInterceptor annotationSharedLoadInterceptor(ApplicationContext context){
    return new SharedLockInterceptor(context);
  }

}
```
 


## 使用说明

组件有三种主要使用方式：

1. try/finally 通用方式，一般常用的方式
2. execute callback 回调方式，类似 JdbcTemplate execute
3. AOP + Annotation 模式，类似 spring 事务


### 全局参数配置
**对于一些全局性配置，如 锁定时间，zk 的namespace等，可以使用全局配置实例配置**
```java
  
  // 全局配置只需要执行一次即可，开发者可以交由 Spring 管理，比如 单例的 PostConstruct 方法等
  // 这里配置了 lockSeconds（锁定时长，如果不设置，则默认20秒）, zk 的node 父节点，默认的服务提供者 (默认提供者必须提供)
  SharedLockEnvironment.getInstance().lockSeconds(20).setConfigurer(ZookeeperLockProvider.class, ZookeeperConfigurer.builder().namespace("/lock-namespace").build()).addDecoratorClasses(defaultDecorators()).setDefaultProvder(provider);
  
```


----

SharedLock 组件内部的锁定义了多种状态(定义在`net.madtiger.lock.SharedLockStatus` 类中)

``` java
/**
 * 共享锁状态
 *
 * @author Fenghu.Shi
 * @version 1.2.0
 */
public enum SharedLockStatus {

  /**
   * 新建
   */
  NEW,

  /**
   * 已锁定
   */
  LOCKED,

  /**
   * 获取所超时
   */
  TIMEOUT,

  /**
   * 取消
   */
  CANCEL,

  /**
   * 获取锁成功后，解锁失败，此阶段正常应该回滚
   */
  UNLOCK_FAIL,

  /**
   * 超时后，已解锁
   */
  TIMEOUT_UNLOCK,

  /**
   * 取消后，已解锁
   */
  CANCEL_UNLOCK,

  /**
   * 释放锁成功
   */
  DONE;

}
```


---- 

创建锁对象使用 Builder模式
```java
//支持链式操作
ISharedLock lock = SharedLockBuilder.builder(LOCK_KEY).build();
```

### try/finally 使用方式

此方式是以往分布式锁使用较多的方式，需要用户手动获取并释放。

**SharedLock 在原始的使用方式基础上增加了一些比较常用/方便的特性**
1. 实现了 ` java.lang.AutoCloseable ` 接口，支持 JDK 8 try-with-resource 特性
2. 返回数据支持链式调用
3. 支持意外回滚

#### 基础使用方式

见 [try/finally 基本使用方式](../../../shared-lock-demo/blob/master/src/main/java/org/shared/lock/demo/DemoController.java) 的 `doTry()`方法

**备注**
1. try-with-resource 和 普通try/finally 使用场景区分
对于所有逻辑都在 try {} 内部执行时，建议使用 try-with-resource 模式，如果需要在 try {} 外部还有根据获取锁状态进行其他业务逻辑时，使用 普通的 try/finally 模式

2. fault callback 使用
如果需要对锁释放失败，进行回退处理业务时，可以通过如下方式使用。
```java

    // 来一个 try-with-resource 模式
    try(ISharedLock lock = SharedLockBuilder.builder(LOCK_KEY).build()) {
      if (lock.tryLock(5, TimeUnit.SECONDS)) {
        System.out.println("执行成功");
        return Flux.just("OK");
      } else {
        return Flux.error(new Throwable("失败了"));
      }
    }

```
3. rollback 模式
当已经获取了锁，由于执行时间过长或者网络等其他原因导致锁没有释放成功，对于安全性较高的系统，在释放锁失败后，需要回退时，可以通过此机制回退。
```java
    
    // 来一个基本模式
    ISharedLock lock = SharedLockBuilder.builder(LOCK_KEY).build();
    Flux<String> result;
    try{
      // 尝试获取所并判断是否锁定成功
      if (lock.tryLock(10, TimeUnit.SECONDS)){
        result = Flux.just("获取锁成功");
      }else {
        result = Flux.just("获取锁失败");
      }
    } finally {
      // 3. 释放锁
      lock.unlock();
      // 这里失败了，咱们 rollback 一下
      if (lock.isRollback()) {
        result = Flux.just("回滚吧");
      }
    }

```


###  execute callback 方式

该方式锁的获取和执行都由系统管理，只有当获取所成功后才执行此方法。

使用方式见 [execute 使用方式](../../../shared-lock-demo/blob/master/src/main/java/org/shared/lock/demo/DemoController.java) 的 `doExecute` 方法。



### AOP Annotation 方式

此方式最简便，只要在需要锁执行的方法上增加 `SharedLock` 注解。


使用方式见 [AOP 使用方式](../../../shared-lock-demo/blob/master/src/main/java/org/shared/lock/demo/DemoService.java) 的 `doAOP` 方法。

**此方式增加了几个特性**
1. 支持降级

`SharedLock#fallbackMethod` 的 配置当获取锁失败后的降级方法。
*注意*：这里降级只正对 获取所失败，如果是业务执行失败，则不会调用此方法，业务失败请 catch 处理。

2. 回滚处理

如果需要对锁释放失败，进行回退处理业务时，可以通过此方式回滚，如果此函数有返回值，且类型和执行方法相同，且此方法返回的数据不为空，则rollback 执行的返回值会覆盖原数据。 
覆盖顺序 rollback -> fallback | callback

3. 其他参数

见 `ISharedLock`


##  特性支持（Decorator/装饰者模式）

系统内部除了基本锁功能，还实现了一些其他特性，如：可重入锁，自旋锁等，此类特性都使用 Decorator 实现。

支持如下：

特性名称|实现类|描述
:--:|:--:|:--:
自旋锁|net.madtiger.lock.decorator.SpinLockDecorator|当使用指定时间段内获取锁时(调用了含有 time和 TimeUnit 参数的方法),如果第一次失败，则会立即获取**3**次，如果失败，则随机休眠200+(300随机)毫秒后继续执行自旋。此服务 建议 Redis Provider 使用。
可重入锁|net.madtiger.lock.decorator.ReentrantLockDecorator|同一个线程内可以多次获取同一把锁，默认均支持，可以使用 `SharedLockEnvironment.getInstance().clearDecoratorClasses()`清空
JVM锁|net.madtiger.lock.decorator.LocalLockDecorator|**未完成，实验中，先误使用**，当有一定概率获取到的锁都是在同一个JVM中（如：节点比较少，5个以内）时，先在jvm中添加一个内存锁，当此锁获取成功后，再正常获取远程锁，解锁同理。

## 客户端（Provider）配置
客户端可以有自己的配置，我们通过 `net.madtiger.lock.provider.IProviderConfigurer`来实现并配置，
如：ZK 的 namespace 配置。

#### 1. 全局配置方式:
```java
    SharedLockEnvironment.getInstance().lockSeconds(20).setConfigurer(ZookeeperLockProvider.class, ZookeeperConfigurer.builder().namespace("/lock-namespace").build());

```
#### 2. 局部方式

```java
    ISharedLock lock = SharedLockBuilder.builder(LOCK_KEY).providerConfigurer(ZookeeperConfigurer.builder().namespace("/lock-namespace")).build();

```


暂时支持的配置如下：

### ZooKeeperProvider
具体实现类`net.madtiger.lock.zk.ZookeeperConfigurer`，支持的属性。

属性|描述
:--:|:--:
namespace|zk 的共享锁 key 父节点，必须以 / 开头





##  版本更新 

* 1.2.0 发布 (2020-02-14)

重构了整体结构，添加了 全局配置和builder等模式

* 1.1.0 发布 (2020-02-12)

支持了可重入锁

* 1.0.6 发布 （2020-02-11）

支持 zookeeper 的 Zookeeper 客户端（CuratorFramework 待完成..）

* 1.0.0 发布 （2020-02-08）

完成了基本框架和 redis 实现。
