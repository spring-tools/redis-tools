package io.github.spring.tools.redis;

import lombok.Getter;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 异步测试接口
 * <p>异步测试接口</p>
 *
 * @author Fenghu.Shi
 * @version 1.0.0
 */
public class AsyncHandler<T> implements Runnable{

    private Supplier<T> supplier;

    /**
     * 返回数据
     */
    @Getter
    private T returnData;

    public AsyncHandler(Supplier<T> supplier){
        Objects.requireNonNull(supplier);
        this.supplier = supplier;
    }


    @Override
    public void run() {
       returnData = supplier.get();
    }


}
