package com.cojac.storyteller.credit.service;

// Issue 1(크레딧 차감 동시성 제어) 벤치마크 아카이브
//
// 비관적 락 / 낙관적 락 / 원자적 UPDATE 세 전략을 저경합·고경합 두 시나리오에서
// 소요시간과 정확성(lost update 여부)을 비교하기 위해 작성했던 테스트다.
// 비교 결과 원자적 UPDATE를 최종 채택했고, 채택되지 않은 두 전략(CreditDeductionBenchmarkService의
// deductPessimistic/deductOptimistic)이 의존하던 ProfileEntity.version(@Version)과
// ProfileRepository.findByIdWithPessimisticLock을 프로덕션 코드에서 제거했기 때문에
// 이 테스트는 더 이상 컴파일되지 않는다. 세 전략을 실제로 비교했다는 근거로 남기기 위해
// 전체를 주석 처리만 하고 삭제하지 않았다.
//
// import com.cojac.storyteller.profile.entity.ProfileEntity;
// import com.cojac.storyteller.profile.repository.ProfileRepository;
// import com.cojac.storyteller.user.entity.LocalUserEntity;
// import com.cojac.storyteller.user.repository.LocalUserRepository;
// import lombok.extern.slf4j.Slf4j;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.DisplayName;
// import org.junit.jupiter.api.Test;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.test.context.SpringBootTest;
//
// import java.time.LocalDate;
// import java.util.concurrent.CompletableFuture;
// import java.util.concurrent.ExecutorService;
// import java.util.concurrent.Executors;
// import java.util.concurrent.TimeUnit;
// import java.util.concurrent.atomic.AtomicInteger;
// import java.util.function.Consumer;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
//
// /**
//  * Issue 1 — 비관적 락 / 낙관적 락 / 원자적 UPDATE("우리 방식") 세 전략의
//  * 정확성(lost update 없음) 및 성능(저경합·고경합 소요시간)을 비교하는 벤치마크.
//  * 클래스 레벨 @Transactional을 두지 않아 각 스레드의 커밋이 실제로 격리된 트랜잭션으로 반영되도록 한다.
//  */
// @SpringBootTest
// @Slf4j
// class CreditDeductionStrategyBenchmarkTest {
//
//     @Autowired
//     private CreditDeductionBenchmarkService benchmarkService;
//
//     @Autowired
//     private ProfileRepository profileRepository;
//
//     @Autowired
//     private LocalUserRepository localUserRepository;
//
//     private static final int THREAD_POOL_SIZE = 50;
//
//     // 저경합: 크레딧이 요청보다 훨씬 많아 잠금/재시도 경쟁이 거의 없는 상태
//     private static final int LOW_CONTENTION_CREDIT = 100;
//     private static final int LOW_CONTENTION_REQUESTS = 20;
//
//     // 고경합: 크레딧이 1개뿐이라 대부분의 요청이 실패하며 경쟁이 극대화된 상태
//     private static final int HIGH_CONTENTION_CREDIT = 1;
//     private static final int HIGH_CONTENTION_REQUESTS = 50;
//
//     private LocalUserEntity localUserEntity;
//
//     @BeforeEach
//     void setup() {
//         localUserEntity = localUserRepository.save(LocalUserEntity.builder()
//                 .username("benchmark-user-" + System.nanoTime())
//                 .encryptedPassword("password")
//                 .email("benchmark-" + System.nanoTime() + "@example.com")
//                 .role("ROLE_USER")
//                 .build());
//     }
//
//     @Test
//     @DisplayName("비관적 락 - 저경합/고경합 성능 및 정확성 측정")
//     void pessimisticLock_Benchmark() throws Exception {
//         runBenchmark("비관적 락 - 저경합", LOW_CONTENTION_CREDIT, LOW_CONTENTION_REQUESTS, benchmarkService::deductPessimistic);
//         runBenchmark("비관적 락 - 고경합", HIGH_CONTENTION_CREDIT, HIGH_CONTENTION_REQUESTS, benchmarkService::deductPessimistic);
//     }
//
//     @Test
//     @DisplayName("낙관적 락 - 저경합/고경합 성능 및 정확성 측정")
//     void optimisticLock_Benchmark() throws Exception {
//         runBenchmark("낙관적 락 - 저경합", LOW_CONTENTION_CREDIT, LOW_CONTENTION_REQUESTS, benchmarkService::deductOptimistic);
//         runBenchmark("낙관적 락 - 고경합", HIGH_CONTENTION_CREDIT, HIGH_CONTENTION_REQUESTS, benchmarkService::deductOptimistic);
//     }
//
//     @Test
//     @DisplayName("원자적 UPDATE(우리 방식) - 저경합/고경합 성능 및 정확성 측정")
//     void atomicUpdate_Benchmark() throws Exception {
//         runBenchmark("원자적 UPDATE - 저경합", LOW_CONTENTION_CREDIT, LOW_CONTENTION_REQUESTS, benchmarkService::deductAtomic);
//         runBenchmark("원자적 UPDATE - 고경합", HIGH_CONTENTION_CREDIT, HIGH_CONTENTION_REQUESTS, benchmarkService::deductAtomic);
//     }
//
//     private void runBenchmark(String label, int initialCredit, int requestCount, Consumer<Integer> deductCall) throws Exception {
//         ProfileEntity profile = profileRepository.save(ProfileEntity.builder()
//                 .name(label)
//                 .pinNumber("1234")
//                 .birthDate(LocalDate.of(2010, 1, 1))
//                 .credit(initialCredit)
//                 .user(localUserEntity)
//                 .build());
//         Integer profileId = profile.getId();
//
//         ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
//         AtomicInteger successCount = new AtomicInteger();
//         AtomicInteger failureCount = new AtomicInteger();
//
//         CompletableFuture<?>[] futures = new CompletableFuture[requestCount];
//         long start = System.nanoTime();
//         for (int i = 0; i < requestCount; i++) {
//             futures[i] = CompletableFuture.runAsync(() -> {
//                 try {
//                     deductCall.accept(profileId);
//                     successCount.incrementAndGet();
//                 } catch (Exception e) {
//                     failureCount.incrementAndGet();
//                 }
//             }, executor);
//         }
//         CompletableFuture.allOf(futures).get(60, TimeUnit.SECONDS);
//         long elapsedMs = (System.nanoTime() - start) / 1_000_000;
//         executor.shutdown();
//
//         ProfileEntity result = profileRepository.findById(profileId).orElseThrow();
//         int expectedSuccess = Math.min(initialCredit, requestCount);
//
//         log.info("[{}] 초기 credit={}, 요청 {}건 -> 성공 {}건, 실패 {}건, 소요시간 {}ms, 최종 credit={}",
//                 label, initialCredit, requestCount, successCount.get(), failureCount.get(), elapsedMs, result.getCredit());
//
//         assertEquals(expectedSuccess, successCount.get(),
//                 label + ": lost update 없이 정확히 min(초기 크레딧, 요청 수)만큼만 성공해야 합니다.");
//         assertEquals(initialCredit - expectedSuccess, result.getCredit(),
//                 label + ": 최종 크레딧은 초기값에서 성공 횟수만큼만 차감되어야 합니다.");
//     }
// }
