package com.liveklass.alimtalk.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 알림 발송(NotificationDispatchService#processClaimed)을 병렬 처리하기 위한 전용 executor.
 * 풀사이즈를 배치사이즈(NotificationWorker#BATCH_SIZE)에 맞춰 잡아서,
 * 배치 전체 처리시간이 순차처리(B*T)가 아니라 병렬처리(ceil(B/poolSize)*T)로 줄어들게 한다.
 * 이래야 STUCK_THRESHOLD가 배치사이즈와 무관하게 "단일 발송 최대 소요시간" 기준으로만 근거를 가질 수 있다.
 * 큐가 가득 차면(외부 API 지속 장애 등) CallerRunsPolicy로 호출 스레드(스케줄러)가 직접 처리해서
 * 작업 유실 없이 자연스러운 백프레셔가 걸리게 한다.
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    private static final int CORE_POOL_SIZE = 20;
    private static final int QUEUE_CAPACITY = 100;

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(CORE_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix("notif-dispatch-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("비동기 알림 처리 중 예기치 못한 오류, method={}", method.getName(), ex);
    }
}
