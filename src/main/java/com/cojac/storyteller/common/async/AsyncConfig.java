package com.cojac.storyteller.common.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {

    @Bean(name = "mailServiceTaskExecutor")
    public ThreadPoolTaskExecutor threadPoolTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(14); // 유지할 스레드 수
        taskExecutor.setMaxPoolSize(20); // 최대 생성 가능한 스레드 수
        taskExecutor.setQueueCapacity(30); // 작업 요청을 담을 수 있는 대기열의 용량
        taskExecutor.setPrestartAllCoreThreads(true); // 애플리케이션 시작 시 모든 코어 스레드를 미리 시작하도록 설정
        taskExecutor.setThreadNamePrefix("mailExecutor-"); // 생성된 스레드의 이름에 접두사 설정
        taskExecutor.initialize();
        return taskExecutor;
    }

    @Bean(name = "s3ServiceTaskExecutor")
    public TaskExecutor taskExecutor() {
        return new SimpleAsyncTaskExecutor();
    }

    // 결제 보상 기록(PaymentRollbackHandler)은 신뢰성이 중요해 s3ServiceTaskExecutor(스레드 무제한 생성)를
    // 그대로 재사용하지 않고, mailServiceTaskExecutor처럼 크기가 제한된 풀을 별도로 둔다
    @Bean(name = "paymentServiceTaskExecutor")
    public ThreadPoolTaskExecutor paymentServiceTaskExecutor() {
        ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
        taskExecutor.setCorePoolSize(4);
        taskExecutor.setMaxPoolSize(8);
        taskExecutor.setQueueCapacity(50);
        taskExecutor.setThreadNamePrefix("paymentExecutor-");
        taskExecutor.initialize();
        return taskExecutor;
    }

}
