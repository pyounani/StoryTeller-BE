package com.cojac.storyteller.common.amazon.eventHandler;

import com.cojac.storyteller.common.amazon.domain.S3DeleteFile;
import com.cojac.storyteller.common.amazon.repository.S3DeleteFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3RollbackHandler {

    private final S3DeleteFileRepository s3DeleteFileRepository;

    /**
     * AFTER_ROLLBACK: 트랜잭션 실패하여 롤백되면 감지해 해당 메서드 실행
     * S3DeleteFile에 삭제해야 할 파일 정보 저장
     * @param event S3 객체에 관한 정보가 담긴 이벤트
     */
    @Async("s3ServiceTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void rollbackS3(UploadS3Event event) {
        try {
            S3DeleteFile s3DeleteFile = S3DeleteFile.create(event.objectPath(), event.getFileName());
            s3DeleteFileRepository.save(s3DeleteFile);
        } catch (Exception e) {
            log.error("[S3RollbackHandler] S3 삭제 큐 저장 실패 - objectPath: {}, fileUid: {}",
                    event.objectPath(), event.getFileName(), e);
        }
    }
}

