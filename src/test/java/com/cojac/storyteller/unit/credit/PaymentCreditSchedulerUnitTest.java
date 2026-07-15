package com.cojac.storyteller.unit.credit;

import com.cojac.storyteller.credit.entity.CreditChargeEntity;
import com.cojac.storyteller.credit.entity.PaymentOutbox;
import com.cojac.storyteller.credit.entity.enums.ChargeStatus;
import com.cojac.storyteller.credit.repository.CreditChargeRepository;
import com.cojac.storyteller.credit.repository.PaymentOutboxRepository;
import com.cojac.storyteller.credit.scheduler.PaymentCreditScheduler;
import com.cojac.storyteller.profile.entity.ProfileEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentCreditSchedulerUnitTest {

    @Mock
    private PaymentOutboxRepository paymentOutboxRepository;

    @Mock
    private CreditChargeRepository creditChargeRepository;

    @InjectMocks
    private PaymentCreditScheduler paymentCreditScheduler;

    @Test
    @DisplayName("Outbox 미처리 건을 정상적으로 크레딧 적립하고 처리 완료로 표시한다")
    void applyPendingCredits_AppliesCreditAndMarksProcessed() {
        // given
        ProfileEntity profile = ProfileEntity.builder().id(1).credit(5).build();
        CreditChargeEntity order = CreditChargeEntity.createPending(profile, "order-1", 1000, 10);
        PaymentOutbox outbox = PaymentOutbox.create("order-1");

        when(paymentOutboxRepository.findByProcessedAtIsNull()).thenReturn(List.of(outbox));
        when(creditChargeRepository.compareAndSetStatus("order-1", ChargeStatus.PENDING, ChargeStatus.SUCCESS)).thenReturn(1);
        when(creditChargeRepository.applyCreditAtomic("order-1")).thenReturn(1);
        when(creditChargeRepository.findByOrderId("order-1")).thenReturn(Optional.of(order));

        // when
        paymentCreditScheduler.applyPendingCredits();

        // then
        assertEquals(15, profile.getCredit());
        assertNotNull(outbox.getProcessedAt());
    }

    @Test
    @DisplayName("이미 적립된 주문이면 크레딧을 다시 적립하지 않고 처리 완료로만 표시한다")
    void applyPendingCredits_SkipsAlreadyAppliedOrder() {
        // given
        PaymentOutbox outbox = PaymentOutbox.create("order-1");
        when(paymentOutboxRepository.findByProcessedAtIsNull()).thenReturn(List.of(outbox));
        when(creditChargeRepository.compareAndSetStatus("order-1", ChargeStatus.PENDING, ChargeStatus.SUCCESS)).thenReturn(0);
        when(creditChargeRepository.applyCreditAtomic("order-1")).thenReturn(0);

        // when
        paymentCreditScheduler.applyPendingCredits();

        // then
        assertNotNull(outbox.getProcessedAt());
        verify(creditChargeRepository, never()).findByOrderId(anyString());
    }

    @Test
    @DisplayName("적립 처리 중 실패하면 미처리 상태로 남아 다음 실행에서 재시도된다")
    void applyPendingCredits_LeavesUnprocessedOnFailure_RetriesNextTick() {
        // given
        ProfileEntity profile = ProfileEntity.builder().id(1).credit(5).build();
        CreditChargeEntity order = CreditChargeEntity.createPending(profile, "order-1", 1000, 10);
        PaymentOutbox outbox = PaymentOutbox.create("order-1");

        when(paymentOutboxRepository.findByProcessedAtIsNull()).thenReturn(List.of(outbox));
        when(creditChargeRepository.applyCreditAtomic("order-1")).thenReturn(1);
        when(creditChargeRepository.findByOrderId("order-1"))
                .thenThrow(new RuntimeException("DB 장애 시뮬레이션"))
                .thenReturn(Optional.of(order));

        // when: 1차 실행 - 실패
        paymentCreditScheduler.applyPendingCredits();

        // then: 미처리 상태 유지, 크레딧 미적립
        assertNull(outbox.getProcessedAt());
        assertEquals(1, outbox.getRetryCount());
        assertEquals(5, profile.getCredit());

        // when: 2차 실행 - 정상 조회로 복구되어 성공
        paymentCreditScheduler.applyPendingCredits();

        // then: 처리 완료, 크레딧 적립
        assertNotNull(outbox.getProcessedAt());
        assertEquals(15, profile.getCredit());
    }
}
