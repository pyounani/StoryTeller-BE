package com.cojac.storyteller.common.amazon.scheduler;

import com.cojac.storyteller.common.amazon.AmazonS3Service;
import com.cojac.storyteller.common.amazon.domain.S3DeleteFile;
import com.cojac.storyteller.common.amazon.repository.S3DeleteFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3CleanupSchedulerTest {

    @Mock
    private S3DeleteFileRepository s3DeleteFileRepository;

    @Mock
    private AmazonS3Service amazonS3Service;

    @InjectMocks
    private S3CleanupScheduler s3CleanupScheduler;

    @Test
    @DisplayName("미처리 건이 100건을 초과해도 청크 단위로 모두 삭제 처리된다")
    void cleanupOrphanedS3Files_ProcessesAllRowsAcrossChunks() {
        // given
        List<S3DeleteFile> unprocessed = createUnprocessedFiles(150);
        when(s3DeleteFileRepository.findByProcessedAtIsNull()).thenReturn(unprocessed);

        // when
        s3CleanupScheduler.cleanupOrphanedS3Files();

        // then
        verify(amazonS3Service, times(150)).deleteS3Idempotent(anyString());
        unprocessed.forEach(file -> assertNotNull(file.getProcessedAt()));
    }

    @Test
    @DisplayName("한 건의 삭제가 실패해도 같은 청크의 나머지 건은 정상 처리된다")
    void cleanupOrphanedS3Files_IsolatesFailureWithinChunk() {
        // given
        List<S3DeleteFile> unprocessed = createUnprocessedFiles(3);
        S3DeleteFile failing = unprocessed.get(1);
        when(s3DeleteFileRepository.findByProcessedAtIsNull()).thenReturn(unprocessed);
        lenient().doThrow(new RuntimeException("S3 delete failed"))
                .when(amazonS3Service).deleteS3Idempotent(failing.getFilePath());

        // when
        s3CleanupScheduler.cleanupOrphanedS3Files();

        // then
        assertNotNull(unprocessed.get(0).getProcessedAt());
        assertNull(failing.getProcessedAt());
        assertNotNull(unprocessed.get(2).getProcessedAt());
    }

    @Test
    @DisplayName("미처리 건이 없으면 S3 삭제를 호출하지 않는다")
    void cleanupOrphanedS3Files_DoesNothingWhenQueueEmpty() {
        // given
        when(s3DeleteFileRepository.findByProcessedAtIsNull()).thenReturn(new ArrayList<>());

        // when
        s3CleanupScheduler.cleanupOrphanedS3Files();

        // then
        verify(amazonS3Service, never()).deleteS3Idempotent(anyString());
    }

    private List<S3DeleteFile> createUnprocessedFiles(int count) {
        List<S3DeleteFile> files = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            files.add(S3DeleteFile.create("https://bucket.s3.amazonaws.com/books/photos/file-" + i + ".png",
                    "file-" + i + ".png"));
        }
        return files;
    }
}
