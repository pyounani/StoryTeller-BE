package com.cojac.storyteller.common.amazon;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AmazonS3Service {

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    private final AmazonS3Client amazonS3Client;

    /**
     * 로컬 경로에 저장
     */
    public String uploadFileToS3(MultipartFile multipartFile, String filePath) {
        // MultipartFile -> File 로 변환
        File uploadFile = null;
        try {
            uploadFile = convert(multipartFile)
                    .orElseThrow(() -> new IllegalArgumentException("[error]: MultipartFile -> 파일 변환 실패"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // S3에 저장된 파일 이름
        String fileName = filePath + "/" + UUID.randomUUID();

        // s3로 업로드 후 로컬 파일 삭제
        String uploadImageUrl = putS3(uploadFile, fileName);
        removeNewFile(uploadFile);
        return uploadImageUrl;
    }

    /**
     * S3로 업로드
     * @param uploadFile : 업로드할 파일
     * @param fileName : 업로드할 파일 이름
     * @return 업로드 경로
     */
    public String putS3(File uploadFile, String fileName) {
        amazonS3Client.putObject(new PutObjectRequest(bucket, fileName, uploadFile).withCannedAcl(
                CannedAccessControlList.PublicRead));
        return amazonS3Client.getUrl(bucket, fileName).toString();
    }

    /**
     * S3에 있는 파일 삭제
     * 영어 파일만 삭제 가능 -> 한글 이름 파일은 안됨
     */
    public void deleteS3(String filePath) throws Exception {
        try{
            String key = filePath.substring(filePath.indexOf(bucket) + bucket.length() + 1);

            try {
                amazonS3Client.deleteObject(bucket, key);
            } catch (AmazonServiceException e) {
                log.info(e.getErrorMessage());
            }

        } catch (Exception exception) {
            log.info(exception.getMessage());
        }
        log.info("[S3Uploader] : S3에 있는 파일 삭제");
    }

    /**
     * S3 삭제 - 이미 삭제되어 없는 경우(404)만 성공으로 간주하고,
     * 그 외 실패(권한/네트워크/파싱 오류 등)는 호출자에게 그대로 전파한다.
     * S3CleanupScheduler처럼 "진짜 실패"와 "이미 처리됨"을 구분해야 하는 호출자 전용.
     */
    public void deleteS3Idempotent(String filePath) {
        String key = filePath.substring(filePath.indexOf(bucket) + bucket.length() + 1);
        try {
            amazonS3Client.deleteObject(bucket, key);
        } catch (AmazonServiceException e) {
            if (e.getStatusCode() == 404) {
                log.info("[S3Uploader] : 이미 삭제된 파일 - key: {}", key);
                return;
            }
            throw e;
        }
    }

    /**
     * S3에서 특정 경로에 있는 사진 목록 가져오기
     *
     * @param folderPath
     * @return 사진 URL 리스트
     */
    public List<String> getAllPhotos(String folderPath) {
        List<String> photoUrls = new ArrayList<>();

        // S3 객체 리스트 요청
        ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucket).withPrefix(folderPath);
        ListObjectsV2Result result;

        do {
            result = amazonS3Client.listObjectsV2(req);

            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                // 파일 URL 생성
                String fileUrl = amazonS3Client.getUrl(bucket, objectSummary.getKey()).toString();
                photoUrls.add(fileUrl);
            }

            req.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return photoUrls;
    }


    /**
     * 로컬에 저장된 파일 지우기
     * @param targetFile : 저장된 파일
     */
    private void removeNewFile(File targetFile) {
        if (targetFile.delete()) {
            log.info("[파일 업로드] : 파일 삭제 성공");
            return;
        }
        log.info("[파일 업로드] : 파일 삭제 실패");
    }


    private Optional<File> convert(MultipartFile file) throws IOException {
        // 로컬에서 저장할 파일 경로 : user.dir => 현재 디렉토리 기준
        String dirPath = System.getProperty("user.dir") + "/" + file.getOriginalFilename();
        File convertFile = new File(dirPath);

        if (convertFile.createNewFile()) {
            // FileOutputStream 데이터를 파일에 바이트 스트림으로 저장
            try (FileOutputStream fos = new FileOutputStream(convertFile)) {
                fos.write(file.getBytes());
            }
            return Optional.of(convertFile);
        }

        return Optional.empty();
    }

    /**
     * 이미지를 바이트 배열로 받아서 S3에 업로드
     * @param imageBytes 이미지의 바이트 배열
     * @return 업로드된 이미지의 URL
     */
    public String uploadImageToS3(byte[] imageBytes, String dirPath) throws IOException {
        String fileName = UUID.randomUUID() + ".png"; // 파일 이름

        // 디렉토리가 존재하지 않으면 생성
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File file = new File(dirPath + "/" + fileName);

        // 이미지 바이트 배열을 파일로 저장
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(imageBytes);
        }

        // 파일을 S3로 업로드
        String uploadImageUrl = putS3(file, "books/photos/" + fileName);

        // 로컬 파일 삭제
        removeNewFile(file);
        return uploadImageUrl;
    }
}
