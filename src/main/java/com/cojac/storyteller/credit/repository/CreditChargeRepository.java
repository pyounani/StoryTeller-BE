package com.cojac.storyteller.credit.repository;

import com.cojac.storyteller.credit.entity.CreditChargeEntity;
import com.cojac.storyteller.credit.entity.enums.ChargeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CreditChargeRepository extends JpaRepository<CreditChargeEntity, Integer> {

    Optional<CreditChargeEntity> findByOrderId(String orderId);

    /**
     * 주문 상태를 원자적으로 전이 (Issue 2: 동일 orderId로 confirmCharge()가 중복 호출돼도
     * PENDING -> SUCCESS/FAILED 전이를 단 하나의 요청만 통과시켜 크레딧 중복 지급을 막는다.
     * 영향 행 수가 0이면 이미 다른 요청이 먼저 처리했다는 뜻 — 재처리 없이 기존 결과를 그대로 반환)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditChargeEntity c SET c.status = :newStatus WHERE c.orderId = :orderId AND c.status = :expectedStatus")
    int compareAndSetStatus(@Param("orderId") String orderId,
                             @Param("expectedStatus") ChargeStatus expectedStatus,
                             @Param("newStatus") ChargeStatus newStatus);

    /**
     * 크레딧 적립을 원자적으로 선점 (Issue 3: 메인 경로와 PaymentCreditScheduler 복구 경로가
     * 동시에 같은 주문을 처리해도 크레딧이 중복 적립되지 않도록, orderId를 멱등키로 삼아
     * 단 한쪽만 적립을 수행하도록 만드는 CAS. 영향 행이 0이면 이미 다른 실행이 적립을 마쳤다는 뜻)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE CreditChargeEntity c SET c.creditAppliedAt = CURRENT_TIMESTAMP WHERE c.orderId = :orderId AND c.creditAppliedAt IS NULL")
    int applyCreditAtomic(@Param("orderId") String orderId);
}
