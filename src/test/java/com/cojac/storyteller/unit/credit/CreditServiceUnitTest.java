package com.cojac.storyteller.unit.credit;

import com.cojac.storyteller.credit.dto.CreditDTO;
import com.cojac.storyteller.credit.repository.CreditChargeRepository;
import com.cojac.storyteller.credit.service.CreditService;
import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.profile.exception.ProfileNotFoundException;
import com.cojac.storyteller.profile.repository.ProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditServiceUnitTest {

    @InjectMocks
    private CreditService creditService;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private CreditChargeRepository creditChargeRepository;

    private ProfileEntity profile;

    @BeforeEach
    void setUp() {
        profile = ProfileEntity.builder().id(1).birthDate(LocalDate.of(2015, 1, 1)).credit(5).build();
    }

    @Test
    @DisplayName("크레딧 잔액 조회 단위 테스트 - 성공")
    void testGetBalance_Success() {
        // given
        when(profileRepository.findById(profile.getId())).thenReturn(Optional.of(profile));

        // when
        CreditDTO result = creditService.getBalance(profile.getId());

        // then
        assertNotNull(result);
        assertEquals(5, result.getCredit());
        verify(profileRepository, times(1)).findById(profile.getId());
    }

    @Test
    @DisplayName("크레딧 잔액 조회 단위 테스트 - 프로필 없음 예외")
    void testGetBalance_ProfileNotFound() {
        // given
        when(profileRepository.findById(profile.getId())).thenReturn(Optional.empty());

        // when & then
        assertThrows(ProfileNotFoundException.class, () -> creditService.getBalance(profile.getId()));
    }

    @Test
    @DisplayName("크레딧 충전 단위 테스트 - 성공")
    void testChargeCredit_Success() {
        // given
        when(profileRepository.findById(profile.getId())).thenReturn(Optional.of(profile));

        // when
        CreditDTO result = creditService.chargeCredit(profile.getId(), 10);

        // then
        assertNotNull(result);
        assertEquals(15, result.getCredit());
        verify(creditChargeRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("크레딧 충전 단위 테스트 - 프로필 없음 예외")
    void testChargeCredit_ProfileNotFound() {
        // given
        when(profileRepository.findById(profile.getId())).thenReturn(Optional.empty());

        // when & then
        assertThrows(ProfileNotFoundException.class, () -> creditService.chargeCredit(profile.getId(), 10));
        verify(creditChargeRepository, never()).save(any());
    }
}
