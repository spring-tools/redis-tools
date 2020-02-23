package io.github.spring.tools.redis;

import io.github.spring.tools.redis.annotation.RedisLock;
import org.springframework.stereotype.Service;

/**
 * 测试注解的demo服务类
 * <p>测试注解的demo服务类</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
@Service
public class DemoService {


    /**
     * 正常业务方法
     * @param name 名称
     * @param age 年龄
     * @return 结果
     */
    @RedisLock(key = "demo-key-#{name}-#{age}", fallbackMethod = "faultMethod", rollbackMethod = "rollbackMethod")
    public String sayHello(String name, int age){
        return String.format("成功->姓名:%s，年龄:%s", name, age);
    }

    /**
     * 失败的回调方法
     * @param name 姓名
     * @param age 年龄
     * @return 结果
     */
    public String faultMethod(String name, int age){
        return String.format("失败->姓名:%s，年龄:%s", name, age);
    }

    /**
     * 回滚的回调方法
     * @param name 姓名
     * @param age 年龄
     * @return 结果
     */
    public String rollbackMethod(String name, int age){
        return String.format("rollback->姓名:%s，年龄:%s", name, age);
    }
}
