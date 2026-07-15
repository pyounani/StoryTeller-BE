package com.cojac.storyteller.credit.event;

/**
 * 토스 결제 승인이 성공한 직후 발행되는 이벤트
 * 트랜잭션이 정상 커밋되면 아무 일도 일어나지 않고, 롤백되면 PaymentRollbackHandler가
 * 이 이벤트를 받아 PaymentOutbox에 보상 기록을 남긴다
 * @param orderId 승인된 결제 주문의 orderId
 */
public record PaymentConfirmedEvent(
        String orderId
) {
}
