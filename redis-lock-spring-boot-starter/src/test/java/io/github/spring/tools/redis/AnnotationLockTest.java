package io.github.spring.tools.redis;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * 注解锁测试类
 * <p>测试RedisLock 注解</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public class AnnotationLockTest extends AbsLockTest{

    private static final String PARAM_NAME = "张三";

    private static final int PARAM_AGE = 25;

    @Autowired
    private DemoService demoService;

    /**
     * 测试 成功
     */
    @Test
    public void testSuccess(){
        assertEquals(String.format("成功->姓名:%s，年龄:%s", PARAM_NAME, PARAM_AGE), demoService.sayHello(PARAM_NAME, PARAM_AGE));
    }

    /**
     * 测试执行失败
     */
    @Test
    public void testFault() throws Exception {
        try(IRedisLock lock = RedisLockBuilder.builder(String.format("demo-key-%s-%s", PARAM_NAME, PARAM_AGE)).lockSeconds(10).build();){
            assertTrue(lock.tryLock());
            async(() -> {
                assertEquals(String.format("失败->姓名:%s，年龄:%s", PARAM_NAME, PARAM_AGE), demoService.sayHello(PARAM_NAME, PARAM_AGE));
                return null;
            });
        }
    }

}
