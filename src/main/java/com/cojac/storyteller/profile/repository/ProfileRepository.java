package com.cojac.storyteller.profile.repository;

import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.user.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProfileRepository extends JpaRepository<ProfileEntity, Integer> {

    List<ProfileEntity> findByUser(UserEntity user);

    /**
     * 크레딧 차감 - 원자적 조건부 UPDATE (Issue 1: 동시 요청 시 lost update 방지)
     * 비관적 락 / 낙관적 락 방식과 성능·정확성을 비교한 뒤 이 방식을 최종 채택함
     * (비교 과정은 CreditDeductionStrategyBenchmarkTest에 주석 처리로 보존됨)
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE ProfileEntity p SET p.credit = p.credit - 1 WHERE p.id = :id AND p.credit >= 1")
    int deductCreditAtomic(@Param("id") Integer id);
}
