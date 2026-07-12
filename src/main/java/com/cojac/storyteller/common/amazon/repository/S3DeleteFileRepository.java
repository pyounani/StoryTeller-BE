package com.cojac.storyteller.common.amazon.repository;

import com.cojac.storyteller.common.amazon.domain.S3DeleteFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface S3DeleteFileRepository extends JpaRepository<S3DeleteFile, Long> {

    // 미처리 건 전체 조회 - 현재 물량 수준에서는 무제한 조회로 충분하나,
    // 장애로 큐가 크게 쌓일 경우 Pageable 기반 조회로 전환 필요
    List<S3DeleteFile> findByProcessedAtIsNull();
}
