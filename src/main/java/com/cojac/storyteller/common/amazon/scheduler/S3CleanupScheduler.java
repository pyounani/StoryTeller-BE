package com.cojac.storyteller.common.amazon.scheduler;

import com.cojac.storyteller.common.amazon.AmazonS3Service;
import com.cojac.storyteller.common.amazon.domain.S3DeleteFile;
import com.cojac.storyteller.common.amazon.repository.S3DeleteFileRepository;
import com.cojac.storyteller.common.amazon.util.PartitionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * s3_delete_queue에 쌓인 미처리 건을 주기적으로 처리해 S3 고아 파일을 삭제한다.
 * (Transactional Outbox 패턴의 relay 역할 - S3RollbackHandler가 적재한 큐를 소비)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class S3CleanupScheduler {

    private static final int CHUNK_SIZE = 100;

    private final S3DeleteFileRepository s3DeleteFileRepository;
    private final AmazonS3Service amazonS3Service;

    @Scheduled(fixedDelay = 60_000)
    public void cleanupOrphanedS3Files() {
        List<S3DeleteFile> unprocessed = s3DeleteFileRepository.findByProcessedAtIsNull();
        if (unprocessed.isEmpty()) {
            return;
        }

        for (List<S3DeleteFile> chunk : PartitionUtils.chunking(unprocessed, CHUNK_SIZE)) {
            processChunk(chunk);
        }
    }

    @Transactional
    public void processChunk(List<S3DeleteFile> chunk) {
        for (S3DeleteFile file : chunk) {
            deleteAndMark(file);
        }
    }

    private void deleteAndMark(S3DeleteFile file) {
        try {
            amazonS3Service.deleteS3Idempotent(file.getFilePath());
            file.markProcessed();
        } catch (Exception e) {
            log.error("[S3CleanupScheduler] S3 파일 삭제 실패 - fileUid: {}, filePath: {}",
                    file.getFileUid(), file.getFilePath(), e);
        }
    }
}
