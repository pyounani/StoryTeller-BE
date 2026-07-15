package com.cojac.storyteller.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ResponseCode {
    /**
     * User
     */
    SUCCESS_REGISTER(HttpStatus.OK, "회원가입을 성공했습니다."),
    SUCCESS_LOGIN(HttpStatus.OK, "로그인을 성공했습니다."),
    SUCCESS_KAKAO_LOGIN(HttpStatus.OK, "카카오 소셜 로그인을 성공했습니다."),
    SUCCESS_GOOGLE_LOGIN(HttpStatus.OK, "구글 소셜 로그인을 성공했습니다."),
    SUCCESS_REISSUE(HttpStatus.OK, "토큰 재발급을 성공했습니다."),
    SUCCESS_TEST(HttpStatus.OK, "테스트를 성공했습니다."),
    SUCCESS_LOGOUT(HttpStatus.OK, "로그아웃을 성공했습니다."),
    SUCCESS_VERIFICATION_USERNAME(HttpStatus.OK, "아이디가 사용 검증 완료했습니다. authResult를 확인해주세요."),
    SUCCESS_VERIFICATION_REQUEST(HttpStatus.OK, "해당 이메일로 인증 코드가 전송되었습니다."),
    SUCCESS_VERIFICATION_CODE(HttpStatus.OK, "인증 코드가 검증 완료했습니다. authResult를 확인해주세요."),

    /**
     * Profile
     */
    SUCCESS_PROFILE_PHOTOS(HttpStatus.OK, "프로필 사진 목록을 성공적으로 조회했습니다."),
    SUCCESS_UPLOAD_PHOTOS(HttpStatus.OK, "프로필 사진들을 성공적으로 업로드했습니다."),
    SUCCESS_CREATE_PROFILE(HttpStatus.OK, "프로필이 성공적으로 생성되었습니다."),
    SUCCESS_VERIFICATION_PIN_NUMBER(HttpStatus.OK, "프로필의 비밀번호를 검증을 완료했습니다. valid를 확인해주세요."),
    SUCCESS_UPDATE_PROFILE(HttpStatus.OK, "프로필이 성공적으로 수정되었습니다."),
    SUCCESS_GET_PROFILE(HttpStatus.OK, "프로필을 성공적으로 조회했습니다."),
    SUCCESS_GET_PROFILE_LIST(HttpStatus.OK, "프로필 목록을 성공적으로 조회했습니다."),
    SUCCESS_DELETE_PROFILE(HttpStatus.OK, "프로필이 성공적으로 삭제되었습니다."),

    /**
     * Book
     */
    SUCCESS_CREATE_BOOK(HttpStatus.CREATED, "동화가 성공적으로 생성되었습니다."),
    SUCCESS_RETRIEVE_BOOKS(HttpStatus.OK, "동화 목록을 성공적으로 조회했습니다."),
    SUCCESS_RETRIEVE_BOOK_DETAILS(HttpStatus.OK, "동화 세부 정보를 성공적으로 조회했습니다."),
    SUCCESS_UPDATE_IS_FAVORITE(HttpStatus.OK, "즐겨찾기 상태를 성공적으로 변경했습니다."),
    SUCCESS_DELETE_BOOK(HttpStatus.OK, "동화를 성공적으로 삭제했습니다."),
    SUCCESS_RETRIEVE_FAVORITE_BOOKS(HttpStatus.OK, "즐겨찾기 목록을 성공적으로 조회했습니다."),
    SUCCESS_RETRIEVE_READING_BOOKS(HttpStatus.OK, "읽고 있는 동화 목록을 성공적으로 조회했습니다."),
    SUCCESS_UPDATE_CURRENT_PAGE(HttpStatus.OK, "현재 읽고 있는 페이지를 성공적으로 변경했습니다."),
    SUCCESS_CREATE_QUIZ(HttpStatus.CREATED, "퀴즈가 성공적으로 생성되었습니다."),

    /**
     * Page
     */
    SUCCESS_RETRIEVE_PAGE_DETAILS(HttpStatus.OK, "페이지 세부 정보를 성공적으로 조회했습니다."),
    SUCCESS_UPDATE_PAGE_IMAGE(HttpStatus.OK, "페이지 이미지를 성공적으로 변경했습니다."),

    /**
     * UnknownWord
     */
    SUCCESS_CREATE_UNKNOWNWORD(HttpStatus.CREATED, "단어가 성공적으로 저장되었습니다"),
    SUCCESS_DELETE_UNKNOWNWORD(HttpStatus.OK, "단어가 성공적으로 삭제되었습니다"),

    /**
     * Setting
     */
    SUCCESS_UPDATE_SETTING(HttpStatus.OK, "책 설정을 성공적으로 변경했습니다"),
    SUCCESS_RETRIEVE_SETTING(HttpStatus.OK, "책 설정을 성공적으로 조회했습니다"),

    /**
     * Credit
     */
    SUCCESS_GET_CREDIT_BALANCE(HttpStatus.OK, "크레딧 잔액을 성공적으로 조회했습니다"),
    SUCCESS_CHARGE_CREDIT(HttpStatus.OK, "크레딧을 성공적으로 충전했습니다"),

    /**
     * Custom status for empty data lists
     */
    SUCCESS_RETRIEVE_EMPTY_LIST(HttpStatus.OK, "데이터 조회를 성공했으나, 목록이 비어 있습니다.");

    private final HttpStatus status;
    private final String message;
}