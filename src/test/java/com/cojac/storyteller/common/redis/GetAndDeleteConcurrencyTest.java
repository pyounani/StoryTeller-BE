package com.cojac.storyteller.common.redis;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 통합 테스트: 비원자적 GET+DEL vs Lua 원자적 GET+DEL 동시성 비교
 *
 * - 테스트 1: 구 코드 방식(getValues → deleteValues 분리) 재현 → Race Condition으로 중복 발급 발생
 * - 테스트 2: Lua getAndDelete → 정확히 1개 스레드만 토큰 획득 (중복 발급 0회)
 *
 */
@SpringBootTest
@Slf4j
class GetAndDeleteConcurrencyTest {

    @Autowired
    private RedisService redisService;

    private static final int THREAD_COUNT = 30;

    @Test
    @DisplayName("비원자적 GET+DEL: Race Condition으로 중복 발급 발생")
    void nonAtomicGetDel_CausesRaceCondition() throws InterruptedException {
        String key = "test:concurrency:nonatomic";
        String token = "token_v1";
        redisService.setValues(key, token, Duration.ofMinutes(5));

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    // 비원자적: GET 후 DEL이 분리되어 있어 여러 스레드가 동시에 통과 가능
                    String stored = redisService.getValues(key);
                    if (redisService.checkExistsValue(stored) && stored.equals(token)) {
                        successCount.incrementAndGet();
                        redisService.deleteValues(key);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        int duplicates = successCount.get() - 1;
        log.info("[비원자 GET+DEL] 중복 발급 횟수: {}회 / {}회 동시 요청", duplicates, THREAD_COUNT);
        assertTrue(successCount.get() > 1,
                "비원자 방식에서 Race Condition으로 중복 발급이 발생해야 합니다. 실제: " + successCount.get() + "회");
    }

    @Test
    @DisplayName("Lua 원자적 GET+DEL: 중복 발급 0회")
    void luaGetAndDelete_ZeroDuplicates() throws InterruptedException {
        String key = "test:concurrency:lua";
        String token = "token_v1";
        redisService.setValues(key, token, Duration.ofMinutes(5));

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_COUNT);

        for (int i = 0; i < THREAD_COUNT; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    // 원자적: Lua 스크립트로 GET+DEL 단일 명령 실행 → 정확히 1개 스레드만 토큰 획득
                    String stored = redisService.getAndDelete(key);
                    if (redisService.checkExistsValue(stored) && token.equals(stored)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        pool.shutdown();

        log.info("[Lua 원자 GET+DEL] {}회 동시 요청 → 중복 발급 0회, 정상 발급 {}회",
                THREAD_COUNT, successCount.get());
        assertEquals(1, successCount.get(),
                "Lua 원자 연산: 30회 동시 요청 중 정확히 1개 스레드만 토큰을 획득해야 합니다.");
    }
}
