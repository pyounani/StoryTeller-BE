package com.cojac.storyteller.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
@Getter
public enum ErrorCode {

    /**
     * 400 BAD_REQUEST: 잘못된 요청
     */
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    INVALID_PIN_NUMBER(HttpStatus.BAD_REQUEST, "잘못된 핀 번호입니다."),
    INVALID_ID_TOKEN(HttpStatus.BAD_REQUEST, "잘못된 구글의 IdToken입니다."),

    /**
     * 401 UNAUTHORIZED: 토큰 만료
     */
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Access 토큰입니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh 토큰입니다."),
    TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "요청 헤더에 토큰이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인에 실패했습니다."),

    /**
     * 404 NOT_FOUND: 리소스를 찾을 수 없음
     */
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    SOCIAL_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "소셜 사용자를 찾을 수 없습니다."),
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "책을 찾을 수 없습니다."),
    PAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "페이지를 찾을 수 없습니다."),
    UNKNOWN_NOT_FOUND(HttpStatus.NOT_FOUND, "단어를 찾을 수 없습니다."),


    /**
     * 405 METHOD_NOT_ALLOWED: 허용되지 않은 Request Method 호출
     */
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "허용되지 않은 메서드입니다."),

    /**
     * 409 CONFLICT: 중복된 이메일
     */
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "중복된 유저 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
    /**
     * 500 INTERNAL_SERVER_ERROR: 내부 서버 오류
     */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류입니다."),
    UNABLE_TO_SEND_EMAIL(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류입니다."),
    NO_SUCH_ALGORITHM(HttpStatus.INTERNAL_SERVER_ERROR, "내부 서버 오류입니다."),

    /**
     * 503 SERVICE_UNAVAILABLE: 일시적 서비스 장애
     */
    REDIS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "일시적으로 서비스를 이용할 수 없습니다."),
    ;

    private final HttpStatus status;
    private final String message;
}
