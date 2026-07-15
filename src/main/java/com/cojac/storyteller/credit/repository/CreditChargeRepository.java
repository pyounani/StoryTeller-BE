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
}
