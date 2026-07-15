package com.cojac.storyteller.credit.event;

import com.cojac.storyteller.credit.entity.PaymentOutbox;
import com.cojac.storyteller.credit.repository.PaymentOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentRollbackHandler {

    private final PaymentOutboxRepository paymentOutboxRepository;

    /**
     * AFTER_ROLLBACK: 토스 승인이 이미 성공한 뒤(PaymentConfirmedEvent 발행 이후) 원본 트랜잭션이
     * 롤백되면(예: 크레딧 반영 중 DB 장애) 감지해 PaymentOutbox에 보상 기록을 남긴다.
     * 정상 커밋되면 이 메서드는 아예 호출되지 않는다.
     * @param event 승인된 결제 주문 정보가 담긴 이벤트
     */
    @Async("paymentServiceTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void rollbackPaymentCredit(PaymentConfirmedEvent event) {
        try {
            PaymentOutbox paymentOutbox = PaymentOutbox.create(event.orderId());
            paymentOutboxRepository.save(paymentOutbox);
        } catch (Exception e) {
            log.error("[PaymentRollbackHandler] 결제 보상 큐 저장 실패 - orderId: {}", event.orderId(), e);
        }
    }
}
