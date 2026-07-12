package com.cojac.storyteller.common.amazon.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "s3_delete_queue")
@Getter
@NoArgsConstructor
public class S3DeleteFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "created_date_time", nullable = false)
    private LocalDateTime createdDateTime; // 생성일시

    @Column(name = "file_path", nullable = false, length = 255)
    private String filePath; // 파일 경로

    @Column(name = "file_uid", nullable = false, length = 255)
    private String fileUid; // 파일 식별자

    @Column(name = "processed_at")
    private LocalDateTime processedAt; // 삭제 처리 완료 일시 (null이면 미처리)

    /**
     * S3DeleteFile 객체 생성
     */
    public static S3DeleteFile create(String filePath, String fileUid) {
        S3DeleteFile s3DeleteFile = new S3DeleteFile();
        s3DeleteFile.filePath = filePath;
        s3DeleteFile.fileUid = fileUid;
        s3DeleteFile.createdDateTime = LocalDateTime.now();
        return s3DeleteFile;
    }

    public void markProcessed() {
        this.processedAt = LocalDateTime.now();
    }
}
