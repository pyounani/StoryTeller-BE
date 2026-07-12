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

}
