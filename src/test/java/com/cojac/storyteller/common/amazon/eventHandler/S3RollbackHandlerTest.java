package com.cojac.storyteller.common.amazon.eventHandler;

import com.cojac.storyteller.common.amazon.repository.S3DeleteFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3RollbackHandlerTest {

    @Mock
    private S3DeleteFileRepository s3DeleteFileRepository;

    @InjectMocks
    private S3RollbackHandler s3RollbackHandler;

    @Test
    @DisplayName("삭제 큐 저장이 실패해도 예외를 전파하지 않고 로그로 처리한다")
    void rollbackS3_DoesNotPropagateException_WhenSaveFails() {
        // given
        UploadS3Event event = new UploadS3Event("https://bucket.s3.amazonaws.com/books/photos/file.png");
        when(s3DeleteFileRepository.save(any())).thenThrow(new RuntimeException("DB down"));

        // when & then
        assertDoesNotThrow(() -> s3RollbackHandler.rollbackS3(event));
    }
}
