package com.cojac.storyteller.credit.service;

import com.cojac.storyteller.common.toss.TossPaymentService;
import com.cojac.storyteller.common.toss.dto.TossConfirmResponseDto;
import com.cojac.storyteller.credit.dto.ConfirmPaymentRequest;
import com.cojac.storyteller.credit.dto.CreditOrderDTO;
import com.cojac.storyteller.credit.entity.CreditChargeEntity;
import com.cojac.storyteller.credit.entity.PaymentOutbox;
import com.cojac.storyteller.credit.entity.enums.ChargeStatus;
import com.cojac.storyteller.credit.repository.CreditChargeRepository;
import com.cojac.storyteller.credit.repository.PaymentOutboxRepository;
import com.cojac.storyteller.credit.scheduler.PaymentCreditScheduler;
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
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * 통합 재현 테스트: 토스 결제 승인은 성공했는데(외부 상태 변경 완료) 그 직후 크레딧 반영 단계에서
 * 로컬 DB 오류가 발생해 confirmCharge() 트랜잭션이 롤백되는 상황을 재현한다.
 * "결제는 됐는데 크레딧은 없는" 상태가 되지 않고, PaymentRollbackHandler가 남긴 보상 기록을
 * PaymentCreditScheduler가 복구해 정확히 1회분만 크레딧을 적립하는지 검증한다 (Issue 3).
 */
@SpringBootTest
@Slf4j
class PaymentCreditReconciliationTest {

    @Autowired
    private CreditService creditService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private PaymentOutboxRepository paymentOutboxRepository;

    @Autowired
    private PaymentCreditScheduler paymentCreditScheduler;

    @Autowired
    private LocalUserRepository localUserRepository;

    @MockBean
    private TossPaymentService tossPaymentService;

    @SpyBean
    private CreditChargeRepository creditChargeRepository;

    private ProfileEntity profileEntity;

    @BeforeEach
    void setup() {
        LocalUserEntity localUserEntity = LocalUserEntity.builder()
                .username("payment-outbox-user")
                .encryptedPassword("password")
                .email("payment-outbox@example.com")
                .role("ROLE_USER")
                .build();
        localUserEntity = localUserRepository.save(localUserEntity);

        profileEntity = ProfileEntity.builder()
                .name("Payment Outbox Test")
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
    @DisplayName("토스 승인 성공 후 크레딧 반영 중 로컬 오류로 롤백돼도, 스케줄러가 보상 기록을 복구해 정확히 1회분만 적립한다")
    void confirmCharge_RollbackAfterTossSuccess_RecoveredByScheduler() throws Exception {
        // given
        CreditOrderDTO order = creditService.createOrder(profileEntity.getId(), 10);
        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                .orderId(order.getOrderId())
                .paymentKey("test-payment-key")
                .amount(order.getAmount())
                .build();

        // 토스 승인 성공 직후 크레딧 반영(applyCreditAtomic) 단계에서 1회 한정으로 DB 장애를 시뮬레이션
        doThrow(new RuntimeException("DB 장애 시뮬레이션"))
                .doCallRealMethod()
                .when(creditChargeRepository).applyCreditAtomic(order.getOrderId());

        // when: 최초 confirmCharge 호출은 실패(롤백)해야 함
        assertThrows(RuntimeException.class, () -> creditService.confirmCharge(request));

        // then: 롤백됐으므로 크레딧은 아직 반영되지 않음
        ProfileEntity afterRollback = profileRepository.findById(profileEntity.getId()).orElseThrow();
        assertEquals(0, afterRollback.getCredit());

        // PaymentRollbackHandler가 비동기로 보상 기록을 남길 때까지 짧게 폴링
        List<PaymentOutbox> outboxRows = waitForOutboxRecord(order.getOrderId());
        assertEquals(1, outboxRows.size(), "롤백 시 PaymentOutbox에 보상 기록이 정확히 1건 생성되어야 합니다.");

        // when: 스케줄러가 보상 기록을 복구
        paymentCreditScheduler.applyPendingCredits();

        // then: 주문 상태가 SUCCESS로 복구되고, 크레딧이 정확히 1회분(10)만 적립됨
        CreditChargeEntity recovered = creditChargeRepository.findByOrderId(order.getOrderId()).orElseThrow();
        assertEquals(ChargeStatus.SUCCESS, recovered.getStatus());
        ProfileEntity afterRecovery = profileRepository.findById(profileEntity.getId()).orElseThrow();
        assertEquals(10, afterRecovery.getCredit(), "스케줄러 복구 후 크레딧이 정확히 1회분만 적립되어야 합니다.");
        assertTrue(paymentOutboxRepository.findByProcessedAtIsNull().isEmpty(), "복구 후 Outbox는 처리 완료 상태여야 합니다.");
    }

    private List<PaymentOutbox> waitForOutboxRecord(String orderId) throws InterruptedException {
        for (int i = 0; i < 20; i++) {
            List<PaymentOutbox> rows = paymentOutboxRepository.findByProcessedAtIsNull().stream()
                    .filter(o -> orderId.equals(o.getPayload()))
                    .toList();
            if (!rows.isEmpty()) {
                return rows;
            }
            Thread.sleep(100);
        }
        return List.of();
    }
}
