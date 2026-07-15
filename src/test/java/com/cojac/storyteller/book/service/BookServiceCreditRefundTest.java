package com.cojac.storyteller.book.service;

import com.cojac.storyteller.common.openAI.ImageGenerationService;
import com.cojac.storyteller.common.openAI.OpenAIService;
import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.profile.repository.ProfileRepository;
import com.cojac.storyteller.user.entity.LocalUserEntity;
import com.cojac.storyteller.user.repository.LocalUserRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 통합 테스트: 크레딧 차감이 REQUIRES_NEW로 즉시 커밋된 이후 OpenAI 호출이 실패했을 때
 * 보상 트랜잭션(refundCreditAtomic)으로 크레딧이 원래 값으로 복구되는지 검증한다.
 */
@SpringBootTest
@Slf4j
class BookServiceCreditRefundTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private LocalUserRepository localUserRepository;

    @MockBean
    private OpenAIService openAIService;

    @MockBean
    private ImageGenerationService imageGenerationService;

    private static final int INITIAL_CREDIT = 5;

    private ProfileEntity profileEntity;

    @BeforeEach
    void setup() {
        LocalUserEntity localUserEntity = LocalUserEntity.builder()
                .username("credit-refund-user")
                .encryptedPassword("password")
                .email("credit-refund@example.com")
                .role("ROLE_USER")
                .build();
        localUserEntity = localUserRepository.save(localUserEntity);

        profileEntity = ProfileEntity.builder()
                .name("Credit Refund Test")
                .pinNumber("1234")
                .birthDate(LocalDate.of(2010, 1, 1))
                .credit(INITIAL_CREDIT)
                .user(localUserEntity)
                .build();
        profileEntity = profileRepository.save(profileEntity);

        when(openAIService.generateStory(anyString(), anyInt()))
                .thenThrow(new RuntimeException("OpenAI 호출 실패"));
    }

    @Test
    @DisplayName("OpenAI 호출 실패 시 이미 커밋된 크레딧 차감이 보상 환불로 원래 값으로 복구되어야 함")
    void createBook_OpenAIFails_CreditIsRefunded() {
        // Given
        Integer profileId = profileEntity.getId();
        String prompt = "Create a story";

        // When & Then
        assertThrows(RuntimeException.class, () -> bookService.createBook(prompt, profileId));

        ProfileEntity result = profileRepository.findById(profileId).orElseThrow();
        log.info("OpenAI 실패 후 크레딧: {} (기대값: {})", result.getCredit(), INITIAL_CREDIT);

        assertEquals(INITIAL_CREDIT, result.getCredit(),
                "차감이 REQUIRES_NEW로 커밋된 뒤 OpenAI 호출이 실패했으므로, 보상 환불로 크레딧이 원래 값으로 복구되어야 합니다.");
    }
}
