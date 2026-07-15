package com.cojac.storyteller.credit.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_outbox")
@Getter
@NoArgsConstructor
public class PaymentOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "created_date_time", nullable = false)
    private LocalDateTime createdDateTime;

    @Column(name = "payload", nullable = false, length = 255)
    private String payload; // 크레딧 적립을 다시 조회/수행하는 데 필요한 orderId

    @Column(name = "processed_at")
    private LocalDateTime processedAt; // 적립 처리 완료 일시 (null이면 미처리)

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    public static PaymentOutbox create(String orderId) {
        PaymentOutbox paymentOutbox = new PaymentOutbox();
        paymentOutbox.payload = orderId;
        paymentOutbox.createdDateTime = LocalDateTime.now();
        paymentOutbox.retryCount = 0;
        return paymentOutbox;
    }

    public void markProcessed() {
        this.processedAt = LocalDateTime.now();
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
