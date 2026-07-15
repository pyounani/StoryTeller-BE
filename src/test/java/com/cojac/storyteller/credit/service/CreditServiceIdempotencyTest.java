package com.cojac.storyteller.credit.service;

import com.cojac.storyteller.common.toss.TossPaymentService;
import com.cojac.storyteller.common.toss.dto.TossConfirmResponseDto;
import com.cojac.storyteller.credit.dto.ConfirmPaymentRequest;
import com.cojac.storyteller.credit.dto.CreditOrderDTO;
import com.cojac.storyteller.credit.repository.CreditChargeRepository;
import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.profile.repository.ProfileRepository;
import com.cojac.storyteller.user.entity.LocalUserEntity;
import com.cojac.storyteller.user.repository.LocalUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 통합 테스트: confirmCharge() 동시(중복) 요청 시, 같은 orderId로 여러 번 승인 요청이 들어와도
 * 크레딧이 정확히 1회분만 지급되는지(멱등성) 검증. 클라이언트 타임아웃 재시도, PG 웹훅 재전송 등으로
 * 동일 orderId가 여러 번 confirm 요청되는 상황을 재현한다. 클래스 레벨 @Transactional을 두지 않아
 * 각 스레드의 커밋이 실제로 격리된 트랜잭션으로 반영되도록 한다.
 */
@SpringBootTest
@Slf4j
class CreditServiceIdempotencyTest {

    @Autowired
    private CreditService creditService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CreditChargeRepository creditChargeRepository;

    @Autowired
    private LocalUserRepository localUserRepository;

    @MockBean
    private TossPaymentService tossPaymentService;

    private static final int REQUEST_COUNT = 10;
    private static final int CREDIT_AMOUNT = 10;

    private ProfileEntity profileEntity;

    @BeforeEach
    void setup() {
        LocalUserEntity localUserEntity = LocalUserEntity.builder()
                .username("credit-idempotency-user")
                .encryptedPassword("password")
                .email("credit-idempotency@example.com")
                .role("ROLE_USER")
                .build();
        localUserEntity = localUserRepository.save(localUserEntity);

        profileEntity = ProfileEntity.builder()
                .name("Credit Idempotency Test")
                .pinNumber("1234")
                .birthDate(LocalDate.of(2010, 1, 1))
                .credit(0)
                .user(localUserEntity)
                .build();
        profileEntity = profileRepository.save(profileEntity);

        when(tossPaymentService.confirmPayment(anyString(), anyString(), anyInt()))
                .thenReturn(new TossConfirmResponseDto());
    }

    @Test
    @DisplayName("같은 orderId로 confirmCharge() 동시(중복) 요청 10건 - 크레딧이 정확히 1회분만 지급되어야 함")
    void confirmCharge_DuplicateRequests_CreditsAppliedOnlyOnce() throws Exception {
        // Given
        CreditOrderDTO order = creditService.createOrder(profileEntity.getId(), CREDIT_AMOUNT);
        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                .orderId(order.getOrderId())
                .paymentKey("test-payment-key")
                .amount(order.getAmount())
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        // When
        CompletableFuture<?>[] futures = new CompletableFuture[REQUEST_COUNT];
        for (int i = 0; i < REQUEST_COUNT; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    creditService.confirmCharge(request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.warn("confirmCharge 실패: {}", e.getMessage());
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        ProfileEntity result = profileRepository.findById(profileEntity.getId()).orElseThrow();
        long orderRowCount = creditChargeRepository.findByOrderId(order.getOrderId()).stream().count();

        log.info("동일 orderId로 동시 confirm {}회 -> 성공 {}회, 실패 {}회, 최종 credit={}, 주문 행 수={}",
                REQUEST_COUNT, successCount.get(), failureCount.get(), result.getCredit(), orderRowCount);

        assertEquals(CREDIT_AMOUNT, result.getCredit(),
                "같은 orderId로 여러 번 승인 요청이 들어와도 크레딧은 정확히 1회분만 지급되어야 합니다 (중복 지급 없음).");
        assertEquals(1, orderRowCount,
                "동일 orderId에 대한 주문 행은 여전히 1개여야 합니다 (중복 행 없음).");
    }
}
