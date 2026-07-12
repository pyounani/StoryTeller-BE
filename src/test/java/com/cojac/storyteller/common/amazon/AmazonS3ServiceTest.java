package com.cojac.storyteller.common.amazon;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
class AmazonS3ServiceTest {

    private static final String BUCKET = "test-bucket";
    private static final String KEY = "books/photos/file.png";
    private static final String FILE_PATH = "https://s3.amazonaws.com/" + BUCKET + "/" + KEY;

    @Mock
    private AmazonS3Client amazonS3Client;

    @InjectMocks
    private AmazonS3Service amazonS3Service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(amazonS3Service, "bucket", BUCKET);
    }

    @Test
    @DisplayName("404(이미 삭제된 파일)는 예외 없이 정상 처리된다")
    void deleteS3Idempotent_DoesNotThrow_WhenAlreadyDeleted() {
        // given
        AmazonServiceException notFound = new AmazonServiceException("Not Found");
        notFound.setStatusCode(404);
        doThrow(notFound).when(amazonS3Client).deleteObject(BUCKET, KEY);

        // when & then
        assertDoesNotThrow(() -> amazonS3Service.deleteS3Idempotent(FILE_PATH));
    }

    @Test
    @DisplayName("404가 아닌 실패는 그대로 전파된다")
    void deleteS3Idempotent_Propagates_WhenRealFailure() {
        // given
        AmazonServiceException forbidden = new AmazonServiceException("Forbidden");
        forbidden.setStatusCode(403);
        doThrow(forbidden).when(amazonS3Client).deleteObject(BUCKET, KEY);

        // when & then
        assertThrows(AmazonServiceException.class, () -> amazonS3Service.deleteS3Idempotent(FILE_PATH));
    }
}
