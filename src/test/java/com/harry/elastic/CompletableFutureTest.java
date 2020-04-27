package com.harry.elastic;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * java8多线程新特性测试
 */
@Slf4j
public class CompletableFutureTest {


    //此executor线程池如果不传,CompletableFuture经测试默认只启用最多3个线程,所以最好自己指定线程数量
    ExecutorService executor = Executors.newFixedThreadPool(15);

    public void execute() {
        long start = System.currentTimeMillis();
        //参数
        List<String> webPageLinks = Arrays.asList("A", "B", "C", "D", "E", "F", "G", "H");
        List<CompletableFuture<Void>> pageContentFutures = webPageLinks.stream()
                .map(webPageLink -> handle(webPageLink))
                .collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                pageContentFutures.toArray(new CompletableFuture[pageContentFutures.size()])
        );

        allFutures.join();
        log.info("所有线程已执行完[{}]", allFutures.isDone());
        allFutures.whenComplete((aVoid, throwable) -> {
            log.info("执行最后一步操作" + aVoid);
            // doSth();
            long end = System.currentTimeMillis();
            log.info("耗时:" + (end - start) / 1000L);
        });
    }

    //executor 可不传入,则默认最多3个线程
    CompletableFuture<Void> handle(String pageLink) {
        //捕捉异常,不会导致整个流程中断
        return CompletableFuture.runAsync(() -> {
            log.info("执行任务" + pageLink);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }, executor).exceptionally(throwable -> {
            log.info("线程[{}]发生了异常, 继续执行其他线程,错误详情[{}]", Thread.currentThread().getName(), throwable.getMessage());
            return null;
        });
    }

    public static void main(String[] args) {
        new CompletableFutureTest().execute();
    }
}
