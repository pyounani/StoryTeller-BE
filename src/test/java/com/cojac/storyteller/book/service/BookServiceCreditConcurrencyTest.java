package com.cojac.storyteller.book.service;

import com.cojac.storyteller.book.repository.BookRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

/**
 * 통합 테스트: createBook() 동시 요청 시 크레딧 차감(this.credit -= 1, 원자적이지 않음)이
 * lost update 없이 정확히 반영되는지 검증. 클래스 레벨 @Transactional을 두지 않아 각 스레드의
 * 커밋이 실제로 격리된 트랜잭션으로 반영되도록 한다.
 */
@SpringBootTest
@Slf4j
class BookServiceCreditConcurrencyTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private LocalUserRepository localUserRepository;

    @MockBean
    private OpenAIService openAIService;

    @MockBean
    private ImageGenerationService imageGenerationService;

    private static final int REQUEST_COUNT = 10;

    private ProfileEntity profileEntity;

    @BeforeEach
    void setup() {
        LocalUserEntity localUserEntity = LocalUserEntity.builder()
                .username("credit-concurrency-user")
                .encryptedPassword("password")
                .email("credit-concurrency@example.com")
                .role("ROLE_USER")
                .build();
        localUserEntity = localUserRepository.save(localUserEntity);

        profileEntity = ProfileEntity.builder()
                .name("Credit Concurrency Test")
                .pinNumber("1234")
                .birthDate(LocalDate.of(2010, 1, 1))
                .credit(1)
                .user(localUserEntity)
                .build();
        profileEntity = profileRepository.save(profileEntity);

        when(openAIService.generateStory(anyString(), anyInt()))
                .thenReturn("Title: 테스트 동화\nContent: 이것은 테스트 동화 내용입니다.");

        when(imageGenerationService.generateAndUploadBookCoverImage(anyString()))
                .thenReturn("http://example.com/test-cover-image.jpg");
    }

    @Test
    @DisplayName("크레딧 1 남은 상태에서 동시 생성 요청 10건 - 정확히 1건만 성공해야 함")
    void createBook_ConcurrentRequests_OnlyOneShouldSucceed() throws Exception {
        // Given
        Integer profileId = profileEntity.getId();
        String prompt = "Create a story";

        ExecutorService executor = Executors.newFixedThreadPool(REQUEST_COUNT);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        // When
        CompletableFuture<?>[] futures = new CompletableFuture[REQUEST_COUNT];
        for (int i = 0; i < REQUEST_COUNT; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    bookService.createBook(prompt, profileId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.warn("createBook 실패: {}", e.getMessage());
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        ProfileEntity result = profileRepository.findById(profileId).orElseThrow();
        long bookCount = bookRepository.findByProfile(result).size();

        log.info("동시 요청 {}회 -> 성공 {}회, 실패 {}회, 최종 credit={}, 생성된 책 수={}",
                REQUEST_COUNT, successCount.get(), failureCount.get(), result.getCredit(), bookCount);

        assertEquals(1, successCount.get(),
                "크레딧이 1개뿐이므로 동시 요청 중 정확히 1건만 성공해야 합니다 (lost update 없음).");
        assertEquals(0, result.getCredit(),
                "차감 후 크레딧은 0이어야 하며 음수가 되어서는 안 됩니다.");
        assertEquals(1, bookCount,
                "성공한 요청 수만큼만 책이 생성되어야 합니다.");
    }
}
