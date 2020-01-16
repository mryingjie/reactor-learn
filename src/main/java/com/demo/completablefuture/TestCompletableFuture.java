package com.demo.completablefuture;

import org.junit.Test;
import reactor.util.function.Tuple2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;

/**
 * created by Yingjie Zheng at 2020-01-15 16:48
 */
public class TestCompletableFuture {

    private ExecutorService executorService = new ThreadPoolExecutor(
            2, //核心线程数2
            Runtime.getRuntime().availableProcessors() * 2, //最大线程=cpu核数*2
            60, //非核心线程600秒内无任务被销毁
            TimeUnit.SECONDS,
            //任务缓存队列最多缓存cpu核数*2个任务
            new LinkedBlockingQueue(Runtime.getRuntime().availableProcessors() * 2),
            // 当线程池的任务缓存队列已满并且线程池中的线程数目达到最大值时由调用线程处理该任务
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Test
    public void test1() {

        List<String> strings = null;
        try {
            strings = CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("hello");
                        return "hello";
                    }, executorService)
                    .thenCombineAsync(
                            CompletableFuture.supplyAsync(() -> {
                                System.out.println("world");
                                return "world";
                            }),
                            (s1, s2) -> Arrays.asList(s1, s2),
                            executorService
                    ).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println(strings);
    }
}
