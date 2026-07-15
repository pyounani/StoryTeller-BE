package com.cojac.storyteller.credit.scheduler;

import com.cojac.storyteller.common.amazon.util.PartitionUtils;
import com.cojac.storyteller.credit.entity.CreditChargeEntity;
import com.cojac.storyteller.credit.entity.PaymentOutbox;
import com.cojac.storyteller.credit.entity.enums.ChargeStatus;
import com.cojac.storyteller.credit.exception.PaymentOrderNotFoundException;
import com.cojac.storyteller.credit.repository.CreditChargeRepository;
import com.cojac.storyteller.credit.repository.PaymentOutboxRepository;
import com.cojac.storyteller.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PaymentOutbox에 쌓인 미처리 건(=토스 승인은 성공했지만 로컬 트랜잭션이 롤백돼 크레딧이
 * 반영되지 못한 결제)을 주기적으로 복구한다.
 * (Transactional Outbox 패턴의 relay 역할 - PaymentRollbackHandler가 적재한 큐를 소비, Issue 3)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCreditScheduler {

    private static final int CHUNK_SIZE = 100;
    private static final int RETRY_ALERT_THRESHOLD = 5;

    private final PaymentOutboxRepository paymentOutboxRepository;
    private final CreditChargeRepository creditChargeRepository;

    @Scheduled(fixedDelay = 60_000)
    public void applyPendingCredits() {
        List<PaymentOutbox> unprocessed = paymentOutboxRepository.findByProcessedAtIsNull();
        if (unprocessed.isEmpty()) {
            return;
        }

        for (List<PaymentOutbox> chunk : PartitionUtils.chunking(unprocessed, CHUNK_SIZE)) {
            processChunk(chunk);
        }
    }

    @Transactional
    public void processChunk(List<PaymentOutbox> chunk) {
        for (PaymentOutbox item : chunk) {
            applyAndMark(item);
        }
    }

    private void applyAndMark(PaymentOutbox item) {
        String orderId = item.getPayload();
        try {
            // 롤백으로 PENDING으로 되돌아갔을 주문 상태를 다시 SUCCESS로 전이 (이미 SUCCESS면 0 반환, 무해)
            creditChargeRepository.compareAndSetStatus(orderId, ChargeStatus.PENDING, ChargeStatus.SUCCESS);

            int applied = creditChargeRepository.applyCreditAtomic(orderId);
            if (applied == 1) {
                CreditChargeEntity order = findOrder(orderId);
                order.getProfile().chargeCredit(order.getChargeAmount());
            }
            item.markProcessed();
        } catch (Exception e) {
            item.incrementRetryCount();
            if (item.getRetryCount() > RETRY_ALERT_THRESHOLD) {
                log.error("[PaymentCreditScheduler] 크레딧 적립 반복 실패 - 수동 처리 필요 - orderId: {}, retryCount: {}",
                        orderId, item.getRetryCount(), e);
            } else {
                log.error("[PaymentCreditScheduler] 크레딧 적립 실패 - orderId: {}, retryCount: {}",
                        orderId, item.getRetryCount(), e);
            }
        }
    }

    private CreditChargeEntity findOrder(String orderId) {
        return creditChargeRepository.findByOrderId(orderId)
                .orElseThrow(() -> new PaymentOrderNotFoundException(ErrorCode.PAYMENT_ORDER_NOT_FOUND));
    }
}
