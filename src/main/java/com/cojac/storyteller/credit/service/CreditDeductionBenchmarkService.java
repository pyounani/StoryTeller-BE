package com.cojac.storyteller.credit.service;

// Issue 1(크레딧 차감 동시성 제어) 벤치마크 아카이브
//
// 비관적 락 / 낙관적 락 / 원자적 UPDATE 세 가지 전략의 정확성·성능을
// CreditDeductionStrategyBenchmarkTest로 비교한 뒤, 원자적 UPDATE
// (ProfileRepository.deductCreditAtomic, BookService.createBook()에 적용됨)를
// 최종 채택했다. 채택되지 않은 두 전략이 의존하던
// ProfileEntity.version(@Version)과 ProfileRepository.findByIdWithPessimisticLock을
// 프로덕션 코드에서 제거했기 때문에, 아래 코드는 더 이상 컴파일되지 않는다.
// 비교 실험을 진행했다는 근거로 남기기 위해 전체를 주석 처리만 하고 삭제하지 않았다.
//
// import com.cojac.storyteller.profile.entity.ProfileEntity;
// import com.cojac.storyteller.profile.exception.InsufficientCreditException;
// import com.cojac.storyteller.profile.exception.ProfileNotFoundException;
// import com.cojac.storyteller.profile.repository.ProfileRepository;
// import com.cojac.storyteller.response.code.ErrorCode;
// import jakarta.persistence.OptimisticLockException;
// import lombok.RequiredArgsConstructor;
// import org.springframework.orm.ObjectOptimisticLockingFailureException;
// import org.springframework.retry.annotation.Backoff;
// import org.springframework.retry.annotation.Retryable;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
//
// /**
//  * Issue 1(크레딧 차감 동시성 제어) — 비관적 락 / 낙관적 락 / 원자적 UPDATE
//  * 세 가지 전략의 정확성·성능을 비교하기 위한 벤치마크 전용 서비스.
//  * 최종적으로 채택된 전략만 BookService.createBook()에 반영될 예정.
//  */
// @Service
// @RequiredArgsConstructor
// public class CreditDeductionBenchmarkService {
//
//     private final ProfileRepository profileRepository;
//
//     /**
//      * 비관적 락: SELECT ... FOR UPDATE로 행을 잠그고 순차 처리
//      */
//     @Transactional
//     public void deductPessimistic(Integer profileId) {
//         ProfileEntity profile = profileRepository.findByIdWithPessimisticLock(profileId)
//                 .orElseThrow(() -> new ProfileNotFoundException(ErrorCode.PROFILE_NOT_FOUND));
//
//         if (profile.getCredit() <= 0) {
//             throw new InsufficientCreditException(ErrorCode.INSUFFICIENT_CREDIT);
//         }
//         profile.deductCredit();
//     }
//
//     /**
//      * 낙관적 락: @Version 충돌 감지 + 재시도 (toggleFavorite()와 동일한 패턴)
//      */
//     @Transactional
//     @Retryable(
//             value = {OptimisticLockException.class, ObjectOptimisticLockingFailureException.class},
//             maxAttempts = 10,
//             backoff = @Backoff(delay = 20, multiplier = 2, maxDelay = 200, random = true)
//     )
//     public void deductOptimistic(Integer profileId) {
//         ProfileEntity profile = profileRepository.findById(profileId)
//                 .orElseThrow(() -> new ProfileNotFoundException(ErrorCode.PROFILE_NOT_FOUND));
//
//         if (profile.getCredit() <= 0) {
//             throw new InsufficientCreditException(ErrorCode.INSUFFICIENT_CREDIT);
//         }
//         profile.deductCredit();
//     }
//
//     /**
//      * 원자적 조건부 UPDATE ("우리 방식"): 확인+차감을 단일 SQL 문으로 처리
//      */
//     @Transactional
//     public void deductAtomic(Integer profileId) {
//         int updatedRows = profileRepository.deductCreditAtomic(profileId);
//         if (updatedRows == 0) {
//             throw new InsufficientCreditException(ErrorCode.INSUFFICIENT_CREDIT);
//         }
//     }
// }
