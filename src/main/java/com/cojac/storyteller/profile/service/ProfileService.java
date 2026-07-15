package com.cojac.storyteller.profile.service;

import com.cojac.storyteller.profile.entity.ProfileEntity;
import com.cojac.storyteller.profile.exception.ProfileNotFoundException;
import com.cojac.storyteller.profile.repository.ProfileRepository;
import com.cojac.storyteller.profile.repository.batch.BatchProfileDelete;
import com.cojac.storyteller.response.code.ErrorCode;
import com.cojac.storyteller.book.entity.BookEntity;
import com.cojac.storyteller.page.entity.PageEntity;
import com.cojac.storyteller.profile.dto.PinCheckResultDTO;
import com.cojac.storyteller.profile.dto.PinNumberDTO;
import com.cojac.storyteller.profile.dto.ProfileDTO;
import com.cojac.storyteller.profile.dto.ProfilePhotoDTO;
import com.cojac.storyteller.common.amazon.AmazonS3Service;
import com.cojac.storyteller.user.entity.UserEntity;
import com.cojac.storyteller.user.exception.InvalidPinNumberException;
import com.cojac.storyteller.user.exception.UserNotFoundException;
import com.cojac.storyteller.book.repository.BookRepository;
import com.cojac.storyteller.user.repository.LocalUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final AmazonS3Service amazonS3Service;
    private final LocalUserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final BatchProfileDelete batchProfileDelete;
    private final BookRepository bookRepository;

    /**
     * S3에서 /profile/photos 경로에 있는 사진 목록 가져오기
     */
    public List<ProfilePhotoDTO> getProfilePhotos() {
        List<String> photoUrls = amazonS3Service.getAllPhotos("profile/photos");
        return photoUrls.stream()
                .map(ProfilePhotoDTO::new) // 각 URL을 DTO로 변환
                .collect(Collectors.toList());
    }

    /**
     * 프로필 생성하기
     */
    @Transactional
    public ProfileDTO createProfile(ProfileDTO profileDTO) {

        // 사용자 아이디로 조회 및 예외 처리
        UserEntity user = userRepository.findById(profileDTO.getUserId())
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        // 핀 번호 유효성 검증
        String pinNumber = profileDTO.getPinNumber();
        if (pinNumber == null || pinNumber.length() != 4 || !pinNumber.matches("\\d+")) {
            throw new InvalidPinNumberException(ErrorCode.INVALID_PIN_NUMBER);
        }

        // 핀 번호 암호화
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hashedPin = encoder.encode(pinNumber);

        // 프로필 생성
        ProfileEntity profileEntity = ProfileEntity.builder()
                .name(profileDTO.getName())
                .birthDate(profileDTO.getBirthDate())
                .imageUrl(profileDTO.getImageUrl())
                .pinNumber(hashedPin)
                .credit(ProfileEntity.DEFAULT_FREE_CREDIT)
                .user(user)
                .build();

        // 프로필 리포지토리 저장
        profileRepository.save(profileEntity);

        // DTO로 매핑
        return ProfileDTO.mapEntityToDTO(profileEntity);
    }

    /**
     * 암호된 프로필 비밀번호 체크하기
     */
    public PinCheckResultDTO verificationPinNumber(Integer profileId, PinNumberDTO pinNumberDTO) {

        // 프로필 아이디로 프로필을 찾기
        ProfileEntity profileEntity = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(ErrorCode.PROFILE_NOT_FOUND));

        // DB에 저장된 암호화된 핀 번호를 가져오기
        String hashedPinFromDB = profileEntity.getPinNumber();

        // 입력된 핀 번호를 가져오기
        String inputPin = pinNumberDTO.getPinNumber();

        // BCryptPasswordEncoder를 사용하여 비밀번호를 검증
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        boolean isValid = encoder.matches(inputPin, hashedPinFromDB);

        return new PinCheckResultDTO(isValid);
    }

    /**
     * 프로필 수정하기
     */
    @Transactional
    public ProfileDTO updateProfile(Integer profileId, ProfileDTO profileDTO) {

        // 프로필 아이디로 프로필을 찾기
        ProfileEntity profileEntity = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(ErrorCode.PROFILE_NOT_FOUND));

        // 핀 번호 유효성 검증
        String pinNumber = profileDTO.getPinNumber();
        if (pinNumber == null || pinNumber.length() != 4 || !pinNumber.matches("\\d+")) {
            throw new InvalidPinNumberException(ErrorCode.INVALID_PIN_NUMBER);
        }

        // 핀 번호 암호화
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        profileDTO.setPinNumber(encoder.encode(pinNumber));

        // 프로필 정보 업데이트
        profileEntity.updateProfile(profileDTO);

        return ProfileDTO.mapEntityToDTO(profileEntity);
    }

    /**
     * 프로필 정보 조회하기
     */
    public ProfileDTO getProfile(Integer profileId) {

        // 프로필 아이디로 프로필을 찾기
        ProfileEntity profileEntity = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(ErrorCode.PROFILE_NOT_FOUND));

        return ProfileDTO.mapEntityToDTO(profileEntity);
    }

    /**
     * 프로필 목록 조회하기
     */
    public List<ProfileDTO> getProfileList(Integer userId) {

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND));

        List<ProfileEntity> profileEntityList = profileRepository.findByUser(user);

        return profileEntityList.stream()
                .map(ProfileDTO::mapEntityToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 프로필 삭제하기
     */
    @Transactional
    public void deleteProfile(Integer profileId) throws Exception {
        ProfileEntity profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ProfileNotFoundException(ErrorCode.PROFILE_NOT_FOUND));

        // 프로필에 연관된 책들 조회
        List<BookEntity> books = bookRepository.findByProfile(profile);

        // 각 책과 페이지의 이미지 삭제
        deleteBookAndPageImages(books);

        // 프로필 삭제
        batchProfileDelete.deleteByProfileId(profileId);
    }

    private void deleteBookAndPageImages(List<BookEntity> books) throws Exception {
        for (BookEntity book : books) {
            deleteImageIfNotNull(book.getCoverImage());

            for (PageEntity page : book.getPages()) {
                if (page.getImage() != null) {
                    amazonS3Service.deleteS3(page.getImage());
                }
            }
        }
    }

    private void deleteImageIfNotNull(String imageUrl) throws Exception {
        if (imageUrl != null) {
            amazonS3Service.deleteS3(imageUrl);
        }
    }

    /**
     * 여러 프로필 사진 업로드
     */
    public void uploadMultipleFilesToS3(MultipartFile[] files) throws IOException {
        for (MultipartFile file : files) {
            amazonS3Service.uploadFileToS3(file, "profile/photos");
        }
    }
}
