package com.cojac.storyteller.unit.credit;

import com.cojac.storyteller.credit.event.PaymentConfirmedEvent;
import com.cojac.storyteller.credit.event.PaymentRollbackHandler;
import com.cojac.storyteller.credit.repository.PaymentOutboxRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentRollbackHandlerUnitTest {

    @Mock
    private PaymentOutboxRepository paymentOutboxRepository;

    @InjectMocks
    private PaymentRollbackHandler paymentRollbackHandler;

    @Test
    @DisplayName("결제 승인 성공 후 트랜잭션 롤백 시 PaymentOutbox에 보상 기록을 남긴다")
    void rollbackPaymentCredit_SavesOutboxRecord() {
        // given
        PaymentConfirmedEvent event = new PaymentConfirmedEvent("order-1");

        // when
        paymentRollbackHandler.rollbackPaymentCredit(event);

        // then
        verify(paymentOutboxRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("보상 기록 저장이 실패해도 예외를 전파하지 않고 로그로 처리한다")
    void rollbackPaymentCredit_DoesNotPropagateException_WhenSaveFails() {
        // given
        PaymentConfirmedEvent event = new PaymentConfirmedEvent("order-1");
        when(paymentOutboxRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        // when & then
        assertDoesNotThrow(() -> paymentRollbackHandler.rollbackPaymentCredit(event));
    }
}
