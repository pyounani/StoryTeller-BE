package com.cojac.storyteller.book.service;

import com.cojac.storyteller.book.entity.BookEntity;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * 통합 테스트: toggleFavorite() 동시 요청 시 낙관적 락(@Version) + @Retryable 기반
 * lost-update 방지 검증. 클래스 레벨 @Transactional을 두지 않아 각 스레드의 커밋이
 * 실제로 격리된 트랜잭션으로 반영되도록 한다.
 */
@SpringBootTest
@Slf4j
class BookServiceConcurrencyTest {

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

    private static final int REQUEST_COUNT = 100;

    private ProfileEntity profileEntity;
    private BookEntity bookEntity;

    @BeforeEach
    void setup() {
        LocalUserEntity localUserEntity = LocalUserEntity.builder()
                .username("concurrency-user")
                .encryptedPassword("password")
                .email("concurrency@example.com")
                .role("ROLE_USER")
                .build();
        localUserEntity = localUserRepository.save(localUserEntity);

        profileEntity = ProfileEntity.builder()
                .name("Concurrency Test")
                .pinNumber("1234")
                .birthDate(LocalDate.of(2010, 1, 1))
                .user(localUserEntity)
                .build();
        profileEntity = profileRepository.save(profileEntity);

        bookEntity = BookEntity.builder()
                .title("Toggle Favorite Concurrency Book")
                .coverImage("coverImage")
                .currentPage(1)
                .isReading(false)
                .isFavorite(false)
                .profile(profileEntity)
                .build();
        bookEntity = bookRepository.save(bookEntity);
    }

    @Test
    @DisplayName("동시 toggleFavorite 요청 100회 - 낙관적 락으로 lost update 없이 정확히 반영")
    void toggleFavorite_ConcurrentRequests_NoLostUpdate() throws Exception {
        // Given
        Integer profileId = profileEntity.getId();
        Integer bookId = bookEntity.getId();
        boolean initialFavorite = bookEntity.isFavorite();
        long initialVersion = bookEntity.getVersion();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failureCount = new AtomicInteger();

        // When
        CompletableFuture<?>[] futures = new CompletableFuture[REQUEST_COUNT];
        for (int i = 0; i < REQUEST_COUNT; i++) {
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    bookService.toggleFavorite(profileId, bookId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                    log.warn("toggleFavorite 실패: {}", e.getMessage());
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).get(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Then
        BookEntity result = bookRepository.findById(bookId).orElseThrow();
        boolean expectedFavorite = successCount.get() % 2 == 1 != initialFavorite;

        log.info("동시 요청 {}회 -> 성공 {}회, 실패 {}회, 최종 isFavorite={}, version={}",
                REQUEST_COUNT, successCount.get(), failureCount.get(), result.isFavorite(), result.getVersion());

        assertEquals(expectedFavorite, result.isFavorite(),
                "성공한 토글 횟수의 홀짝에 따른 기대값과 실제 최종 상태가 일치해야 합니다 (lost update 없음).");
        assertEquals(initialVersion + successCount.get(), result.getVersion(),
                "성공한 저장 횟수만큼 정확히 version이 증가해야 합니다.");
    }
}
