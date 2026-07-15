package com.cojac.storyteller.credit.service;

import com.cojac.storyteller.credit.dto.CreditDTO;
import com.cojac.storyteller.credit.entity.CreditChargeEntity;
import com.cojac.storyteller.credit.repository.CreditChargeRepository;
import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.profile.exception.ProfileNotFoundException;
import com.cojac.storyteller.profile.repository.ProfileRepository;
import com.cojac.storyteller.response.code.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreditService {

    private final ProfileRepository profileRepository;
    private final CreditChargeRepository creditChargeRepository;

    /**
     * 크레딧 잔액 조회
     */
    public CreditDTO getBalance(Integer profileId) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(ErrorCode.PROFILE_NOT_FOUND));

        return CreditDTO.toDto(profile);
    }

    /**
     * 크레딧 충전 (mock 승인 - 항상 성공 처리)
     */
    @Transactional
    public CreditDTO chargeCredit(Integer profileId, Integer amount) {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(ErrorCode.PROFILE_NOT_FOUND));

        profile.chargeCredit(amount);
        creditChargeRepository.save(CreditChargeEntity.createSuccess(profile, amount));

        return CreditDTO.toDto(profile);
    }
}
