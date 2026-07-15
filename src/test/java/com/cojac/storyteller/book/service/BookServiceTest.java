package com.cojac.storyteller.book.service;

import com.cojac.storyteller.book.dto.BookDTO;
import com.cojac.storyteller.book.dto.BookDetailResponseDTO;
import com.cojac.storyteller.book.dto.BookListResponseDTO;
import com.cojac.storyteller.book.dto.QuizResponseDTO;
import com.cojac.storyteller.book.entity.BookEntity;
import com.cojac.storyteller.book.exception.BookNotFoundException;
import com.cojac.storyteller.book.repository.BookRepository;
import com.cojac.storyteller.common.openAI.ImageGenerationService;
import com.cojac.storyteller.common.openAI.OpenAIService;
import com.cojac.storyteller.page.entity.PageEntity;
import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.profile.exception.ProfileNotFoundException;
import com.cojac.storyteller.profile.repository.ProfileRepository;
import com.cojac.storyteller.user.entity.LocalUserEntity;
import com.cojac.storyteller.user.repository.LocalUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 통합 테스트 클래스
 *
 * 이 클래스는 여러 구성 요소(서비스, 데이터베이스 등) 간의 상호작용을 검증하기 위한 통합 테스트를 포함합니다.
 *
 * 주요 특징:
 * - 실제 데이터베이스와 리포지토리를 사용하여 테스트를 수행합니다.
 * - 외부 서비스와의 의존성을 최소화하기 위해 모의 객체를 활용합니다.
 *
 * 통합 테스트는 여러 구성 요소 간의 상호작용을 검증하므로, 일반적으로 단위 테스트보다 느릴 수 있습니다.
 *
 * 테스트 전략:
 * - 간단한 기능이나 로직에 대한 테스트는 단위 테스트를 사용하십시오.
 * - 시스템의 전체적인 동작 및 상호작용을 검증하기 위해 통합 테스트를 활용하십시오.
 *
 */

@SpringBootTest
@Transactional
public class BookServiceTest {

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private LocalUserRepository localUserRepository;

    // 외부 서비스를 모의 객체로 대체
    @MockBean
    private OpenAIService openAIService;

    @MockBean
    private ImageGenerationService imageGenerationService;

    private ProfileEntity profileEntity;
    private LocalUserEntity localUserEntity;

    @BeforeEach
    public void setup() {
        localUserEntity = LocalUserEntity.builder()
                .username("username")
                .encryptedPassword("password")
                .email("email.com")
                .role("ROLE_USER")
                .build();
        localUserEntity = localUserRepository.save(localUserEntity);

        profileEntity = ProfileEntity.builder()
                .name("Test name")
                .pinNumber("1234")
                .birthDate(LocalDate.of(2010, 1, 1))
                .credit(5)
                .user(localUserEntity)
                .build();
        profileEntity = profileRepository.save(profileEntity);

        // 모의 객체의 동작 정의
        when(openAIService.generateStory(anyString(), anyInt()))
                .thenReturn("Title: 테스트 동화\nContent: 이것은 테스트 동화 내용입니다.");

        when(openAIService.generateQuiz(anyString(), anyInt()))
                .thenReturn("Quiz Question: 이것은 테스트 퀴즈 내용입니다.");

        when(imageGenerationService.generateAndUploadBookCoverImage(anyString()))
                .thenReturn("http://example.com/test-cover-image.jpg");

        when(imageGenerationService.generateAndUploadPageImage(anyString()))
                .thenReturn("http://example.com/test-page-image.jpg");
    }

    private BookEntity createBook(String title, boolean isReading, boolean isFavorite) {
        BookEntity book = BookEntity.builder()
                .title(title)
                .coverImage("coverImage")
                .currentPage(1)
                .isReading(isReading)
                .isFavorite(isFavorite)
                .profile(profileEntity)
                .build();
        return bookRepository.save(book);
    }

    /**
     * 동화 생성
     */
    @Test
    @DisplayName("동화 생성하기 통합 테스트 - 성공")
    public void testCreateBook_Success() {
        // Given
        String prompt = "Create a story";
        Integer profileId = profileEntity.getId();

        // When
        BookDTO bookDTO = bookService.createBook(prompt, profileId);

        // Then
        assertNotNull(bookDTO);
        assertEquals(profileId, bookDTO.getProfileId());
        assertEquals("테스트 동화", bookDTO.getTitle());
        assertEquals("http://example.com/test-cover-image.jpg", bookDTO.getCoverImage());

        BookEntity savedBook = bookRepository.findById(bookDTO.getId()).orElse(null);
        assertNotNull(savedBook);
        assertEquals(bookDTO.getTitle(), savedBook.getTitle());
        assertEquals(profileEntity.getId(), savedBook.getProfile().getId());
    }

    @Test
    @DisplayName("동화 생성하기 통합 테스트 - 프로필 없음 예외")
    void testCreateBook_ProfileNotFound() {
        // given
        String prompt = "Create a story";

        // when & then
        Integer invalidProfileId = -1;
        assertThrows(ProfileNotFoundException.class, () -> bookService.createBook(prompt, invalidProfileId));
    }

    /**
     * 책 목록 조회
     */
    @Test
    @DisplayName("책 목록 페이지 조회하기 통합 테스트 - 성공")
    public void testGetBooksPage_Success() {
        // Given
        createBook("Book 1", true, false);
        createBook("Book 2", true, false);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<BookListResponseDTO> books = bookService.getBooksPage(profileEntity.getId(), pageable);

        // Then
        assertNotNull(books);
        assertEquals(2, books.size());
    }

    @Test
    @DisplayName("책 목록 페이지 조회하기 통합 테스트 - 프로필 없음 예외")
    void testGetBooksPage_ProfileNotFound() {
        // given
        createBook("Book 1", true, false);
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        Integer invalidProfileId = -1;
        assertThrows(ProfileNotFoundException.class, () -> bookService.getBooksPage(invalidProfileId, pageable));
    }

    /**
     * 즐겨찾기 책 목록 조회
     */
    @Test
    @DisplayName("즐겨찾기 책 조회하기 통합 테스트 - 성공")
    public void testGetFavoriteBooks_Success() {
        // Given
        createBook("Favorite Book", true, true);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<BookListResponseDTO> favoriteBooks = bookService.getFavoriteBooks(profileEntity.getId(), pageable);

        // Then
        assertNotNull(favoriteBooks);
        assertEquals(1, favoriteBooks.size());
        assertEquals("Favorite Book", favoriteBooks.get(0).getTitle());
    }

    @Test
    @DisplayName("즐겨찾기 책 조회하기 통합 테스트 - 프로필 없음 예외")
    void testGetFavoriteBooks_ProfileNotFound() {
        // given
        createBook("Book 1", true, true);
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        Integer invalidProfileId = -1;
        assertThrows(ProfileNotFoundException.class, () -> bookService.getFavoriteBooks(invalidProfileId, pageable));
    }

    /**
     * 읽고 있는 책 목록 조회
     */
    @Test
    @DisplayName("읽고 있는 책 조회하기 통합 테스트 - 성공")
    public void testGetReadingBooks_Success() {
        // Given
        createBook("Reading Book", true, true);

        Pageable pageable = PageRequest.of(0, 10);

        // When
        List<BookListResponseDTO> readingBooks = bookService.getReadingBooks(profileEntity.getId(), pageable);

        // Then
        assertNotNull(readingBooks);
        assertEquals(1, readingBooks.size());
        assertEquals("Reading Book", readingBooks.get(0).getTitle());
    }

    @Test
    @DisplayName("읽고 있는 책 조회하기 통합 테스트 - 프로필 없음 예외")
    void testGetReadingBook_ProfileNotFound() {
        // given
        createBook("Book 1", true, true);
        Pageable pageable = PageRequest.of(0, 10);

        // when & then
        Integer invalidProfileId = -1;
        assertThrows(ProfileNotFoundException.class, () -> bookService.getReadingBooks(invalidProfileId, pageable));
    }

    /**
     * 책 세부 조회
     */
    @Test
    @DisplayName("책 상세 조회하기 통합 테스트 - 성공")
    public void testGetBookDetail_Success() {
        // Given
        BookEntity book = createBook("Detail Book", true, true);

        // When
        BookDetailResponseDTO bookDetail = bookService.getBookDetail(profileEntity.getId(), book.getId());

        // Then
        assertNotNull(bookDetail);
        assertEquals("Detail Book", bookDetail.getTitle());
    }

    @Test
    @DisplayName("책 상세 조회하기 통합 테스트 - 책 없음 예외")
    void testGetBookDetail_BookNotFound() {
        // given
        BookEntity book = createBook("Detail Book", true, true);

        // when & then
        Integer invalidBookId = -1;
        assertThrows(BookNotFoundException.class, () -> bookService.getBookDetail(profileEntity.getId(), invalidBookId));
    }

    /**
     * 즐겨찾기 토글 기능 추가
     */
    @Test
    @DisplayName("즐겨찾기 상태 토글하기 통합 테스트 - 성공")
    public void testToggleFavorite_Success() {
        // Given
        BookEntity book = createBook("Toggle Favorite Book", true, false);

        // When
        Boolean newFavoriteStatus = bookService.toggleFavorite(profileEntity.getId(), book.getId());

        // Then
        assertTrue(newFavoriteStatus);
        assertTrue(bookRepository.findById(book.getId()).get().isFavorite());
    }

    @Test
    @DisplayName("즐겨찾기 상태 토글하기 통합 테스트 - 책 없음 예외")
    void testToggleFavorite_BookNotFound() {
        // given
        BookEntity book = createBook("Toggle Favorite Book", true, false);

        // when & then
        Integer invalidBookId = -1;
        assertThrows(BookNotFoundException.class, () -> bookService.toggleFavorite(profileEntity.getId(), invalidBookId));
    }

    /**
     * 책 삭제 기능
     */
    @Test
    @DisplayName("책 삭제하기 통합 테스트 - 성공")
    public void testDeleteBook_Success() throws Exception {
        // Given
        BookEntity book = createBook("Delete Book", true, true);

        // When
        bookService.deleteBook(profileEntity.getId(), book.getId());

        // Then
        assertThrows(BookNotFoundException.class, () -> bookService.getBookDetail(profileEntity.getId(), book.getId()));
    }

    @Test
    @DisplayName("책 삭제하기 통합 테스트 - 프로필 없음 예외")
    void testDeleteBook_ProfileNotFound() {
        // given
        BookEntity book = createBook("Delete Book", true, true);

        // when & then
        Integer invalidProfileId = -1;
        assertThrows(ProfileNotFoundException.class, () -> bookService.deleteBook(invalidProfileId, book.getId()));
    }

    @Test
    @DisplayName("책 삭제하기 통합 테스트 - 책 없음 예외")
    void testDeleteBook_BookNotFound() {
        // given
        BookEntity book = createBook("Delete Book", true, true);

        // when & then
        Integer invalidBookId = -1;
        assertThrows(BookNotFoundException.class, () -> bookService.deleteBook(profileEntity.getId(), invalidBookId));
    }

    /**
     * 현재 읽고 있는 페이지 업데이트
     */
    @Test
    @DisplayName("현재 페이지 업데이트하기 통합 테스트 - 성공")
    public void testUpdateCurrentPage_Success() {
        // Given
        BookEntity book = createBook("Update Current Page Book", true, true);

        PageEntity page1 = PageEntity.builder()
                .pageNumber(1)
                .content("페이지 1 내용")
                .image("http://example.com/page1-image.jpg")
                .book(book)
                .build();

        PageEntity page2 = PageEntity.builder()
                .pageNumber(2)
                .content("페이지 2 내용")
                .image("http://example.com/page2-image.jpg")
                .book(book)
                .build();
        book.getPages().addAll(Arrays.asList(page1, page2));
        bookRepository.save(book);

        // When
        BookDTO updatedBook = bookService.updateCurrentPage(profileEntity.getId(), book.getId(), 2);

        // Then
        assertNotNull(updatedBook);
        assertEquals(2, updatedBook.getCurrentPage());
    }

    @Test
    @DisplayName("현재 페이지 업데이트하기 단위 테스트 - 프로필 없음 예외")
    void testUpdateCurrentPage_ProfileNotFound() {
        // Given
        BookEntity book = createBook("Update Current Page Book", true, true);

        PageEntity page1 = PageEntity.builder()
                .pageNumber(1)
                .content("페이지 1 내용")
                .image("http://example.com/page1-image.jpg")
                .book(book)
                .build();

        PageEntity page2 = PageEntity.builder()
                .pageNumber(2)
                .content("페이지 2 내용")
                .image("http://example.com/page2-image.jpg")
                .book(book)
                .build();
        book.getPages().addAll(Arrays.asList(page1, page2));
        bookRepository.save(book);

        // when & then
        Integer invalidProfileId = -1;
        assertThrows(ProfileNotFoundException.class, () -> bookService.updateCurrentPage(invalidProfileId, book.getId(), 2));
    }

    @Test
    @DisplayName("현재 페이지 업데이트하기 단위 테스트 - 책 없음 예외")
    void testUpdateCurrentPage_BookNotFound() {
        // Given
        BookEntity book = createBook("Update Current Page Book", true, true);

        PageEntity page1 = PageEntity.builder()
                .pageNumber(1)
                .content("페이지 1 내용")
                .image("http://example.com/page1-image.jpg")
                .book(book)
                .build();

        PageEntity page2 = PageEntity.builder()
                .pageNumber(2)
                .content("페이지 2 내용")
                .image("http://example.com/page2-image.jpg")
                .book(book)
                .build();
        book.getPages().addAll(Arrays.asList(page1, page2));

        bookRepository.save(book);

        // when & then
        Integer invalidBookId = -1;
        assertThrows(BookNotFoundException.class, () -> bookService.updateCurrentPage(profileEntity.getId(), invalidBookId, 2));
    }

    /**
     * 퀴즈만 생성
     */
    @Test
    @DisplayName("퀴즈 생성하기 통합 테스트 - 성공")
    public void testCreateQuiz_Success() {
        // Given
        BookEntity book = createBook("Quiz Book", true, true);

        // When
        QuizResponseDTO quizResponse = bookService.createQuiz(profileEntity.getId(), book.getId());

        // Then
        assertNotNull(quizResponse);
        assertEquals("Quiz Question: 이것은 테스트 퀴즈 내용입니다.", quizResponse.getQuestion());
    }

    @Test
    @DisplayName("퀴즈 생성하기 통합 테스트 - 프로필 없음 예외")
    void testCreateQuiz_ProfileNotFound() {
        // Given
        BookEntity book = createBook("Quiz Book", true, true);

        // when & then
        Integer invalidProfileId = -1;
        assertThrows(ProfileNotFoundException.class, () -> bookService.createQuiz(invalidProfileId, book.getId()));
    }

    @Test
    @DisplayName("퀴즈 생성하기 통합 테스트 - 책 없음 예외")
    void testCreateQuiz_BookNotFound() {
        // given
        BookEntity book = createBook("Quiz Book", true, true);

        // when & then
        Integer invalidBookId = -1;
        assertThrows(BookNotFoundException.class, () -> bookService.createQuiz(profileEntity.getId(), invalidBookId));
    }
}
