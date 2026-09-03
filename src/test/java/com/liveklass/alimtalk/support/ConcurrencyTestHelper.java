package com.liveklass.alimtalk.support;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * threadCount개 스레드가 동시에 task를 실행하도록 시작 시점을 맞춰준다.
 * 각 스레드가 준비될 때까지 기다렸다가(readyLatch) 한 번에 출발(startLatch)시켜서
 * 실제 동시 요청에 가까운 레이스 조건을 재현한다.
 */
public final class ConcurrencyTestHelper {

    private ConcurrencyTestHelper() {
    }

    public static void runConcurrently(int threadCount, Runnable task) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                readyLatch.countDown();
                try {
                    startLatch.await();
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        readyLatch.await();
        startLatch.countDown();
        doneLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
    }
}
