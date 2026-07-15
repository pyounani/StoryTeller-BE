package com.cojac.storyteller.profile.repository;

import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Integer> {

    List<ProfileEntity> findByUser(UserEntity user);

    /**
     * 크레딧 차감 - 원자적 조건부 UPDATE (Issue 1: 동시 요청 시 lost update 방지)
     * 비관적 락 / 낙관적 락 방식과 성능·정확성을 비교한 뒤 이 방식을 최종 채택함
     * (비교 과정은 CreditDeductionStrategyBenchmarkTest에 주석 처리로 보존됨)
     * REQUIRES_NEW로 분리해 즉시 커밋·락 반납하여, 이후 OpenAI/DALL-E 호출이 진행되는 동안
     * 크레딧 행 락을 붙들고 있지 않도록 함 (실패 시 보상 환불은 refundCreditAtomic 참고)
     */
    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE ProfileEntity p SET p.credit = p.credit - 1 WHERE p.id = :id AND p.credit >= 1")
    int deductCreditAtomic(@Param("id") Integer id);

    /**
     * 크레딧 환불 - deductCreditAtomic() 커밋 이후 OpenAI/DALL-E 호출이 실패했을 때의 보상 트랜잭션
     * (outer 트랜잭션 롤백으로는 이미 커밋된 차감을 되돌릴 수 없어 명시적으로 필요)
     */
    @Modifying(clearAutomatically = true)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Query("UPDATE ProfileEntity p SET p.credit = p.credit + 1 WHERE p.id = :id")
    void refundCreditAtomic(@Param("id") Integer id);
}
