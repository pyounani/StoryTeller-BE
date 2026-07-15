package com.cojac.storyteller.common.exception;

import com.cojac.storyteller.book.exception.BookNotFoundException;
import com.cojac.storyteller.page.exception.PageNotFoundException;
import com.cojac.storyteller.profile.exception.InsufficientCreditException;
import com.cojac.storyteller.profile.exception.ProfileNotFoundException;
import com.cojac.storyteller.response.code.ErrorCode;
import com.cojac.storyteller.response.dto.ErrorResponseDTO;
import com.cojac.storyteller.unknownWord.exception.UnknownWordNotFoundException;
import com.cojac.storyteller.user.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 컨트롤러 전역에서 발생하는 예외를 처리
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 입력값 검증
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponseDTO> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        StringBuilder builder = new StringBuilder();
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            builder.append(fieldError.getDefaultMessage());
        }

        log.error("handleMethodArgumentNotValidException : {}", builder.toString());
        return ResponseEntity
                .status(ErrorCode.BAD_REQUEST.getStatus().value())
                .body(new ErrorResponseDTO(ErrorCode.BAD_REQUEST, builder.toString()));
    }

    /**
     * User
     */
    @ExceptionHandler(DuplicateUsernameException.class)
    protected ResponseEntity<ErrorResponseDTO> handleDuplicateUsernameException(final DuplicateUsernameException e) {
        log.error("handleDuplicateUsernameException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(ErrorCode.DUPLICATE_USERNAME.getStatus().value())
                .body(new ErrorResponseDTO(ErrorCode.DUPLICATE_USERNAME));
    }

    @ExceptionHandler(SocialUserNotFoundException.class)
    protected ResponseEntity<ErrorResponseDTO> handleSocialUserNotFoundException(final SocialUserNotFoundException e) {
        log.error("handleSocialUserNotFoundException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(ErrorCode.SOCIAL_USER_NOT_FOUND.getStatus().value())
                .body(new ErrorResponseDTO(ErrorCode.SOCIAL_USER_NOT_FOUND));
    }

    @ExceptionHandler(UsernameExistsException.class)
    protected ResponseEntity<ErrorResponseDTO> handleUsernameExistsException(final UsernameExistsException e) {
        log.error("handleUsernameExistsException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(ErrorCode.DUPLICATE_USERNAME.getStatus().value())
                .body(new ErrorResponseDTO(ErrorCode.DUPLICATE_USERNAME));
    }

    @ExceptionHandler(EmailSendingException.class)
    protected ResponseEntity<ErrorResponseDTO> handleEmailSendingException(final EmailSendingException e) {
        log.error("handleEmailSendingException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(ErrorCode.UNABLE_TO_SEND_EMAIL.getStatus().value())
                .body(new ErrorResponseDTO(ErrorCode.UNABLE_TO_SEND_EMAIL));
    }

    @ExceptionHandler(BusinessLogicException.class)
    protected ResponseEntity<ErrorResponseDTO> handleBusinessLogicException(final BusinessLogicException e) {
        log.error("handleBusinessLogicException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(ErrorCode.NO_SUCH_ALGORITHM.getStatus().value())
                .body(new ErrorResponseDTO(ErrorCode.NO_SUCH_ALGORITHM));
    }

    @ExceptionHandler(InvalidIdTokenException.class)
    protected ResponseEntity<ErrorResponseDTO> handleInvalidIdTokenException(final InvalidIdTokenException e) {
        log.error("handleInvalidIdTokenException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(ErrorCode.NO_SUCH_ALGORITHM.getStatus().value())
                .body(new ErrorResponseDTO(ErrorCode.NO_SUCH_ALGORITHM));
    }


    /**
     * Profile
     */
    @ExceptionHandler(InvalidPinNumberException.class)
    protected ResponseEntity<ErrorResponseDTO> handleInvalidPinNumberException(final InvalidPinNumberException e) {
        log.error("handleInvalidPinNumberException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_PIN_NUMBER.getStatus().value())
                .body(new ErrorResponseDTO(ErrorCode.INVALID_PIN_NUMBER));
    }

    @ExceptionHandler(InsufficientCreditException.class)
    protected ResponseEntity<ErrorResponseDTO> handleInsufficientCreditException(final InsufficientCreditException e) {
        log.error("handleInsufficientCreditException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus().value())
                .body(new ErrorResponseDTO(e.getErrorCode()));
    }

    /**
     * Book
     */
    @ExceptionHandler(ProfileNotFoundException.class)
    protected ResponseEntity<ErrorResponseDTO> handleProfileNotFoundException(final ProfileNotFoundException e) {
        log.error("handleProfileNotFoundException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus().value())
                .body(new ErrorResponseDTO(e.getErrorCode()));
    }

    @ExceptionHandler(BookNotFoundException.class)
    protected ResponseEntity<ErrorResponseDTO> handleBookNotFoundException(final BookNotFoundException e) {
        log.error("handleBookNotFoundException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus().value())
                .body(new ErrorResponseDTO(e.getErrorCode()));
    }

    /**
     * Page
     */
    @ExceptionHandler(PageNotFoundException.class)
    protected ResponseEntity<ErrorResponseDTO> handlePageNotFoundException(final PageNotFoundException e) {
        log.error("handlePageNotFoundException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus().value())
                .body(new ErrorResponseDTO(e.getErrorCode()));
    }

    /**
     * UnknownWord
     */
    @ExceptionHandler(UnknownWordNotFoundException.class)
    protected ResponseEntity<ErrorResponseDTO> handleUnknownWordNotFoundException(final UnknownWordNotFoundException e) {
        log.error("handleDuplicateUsernameException : {}", e.getErrorCode().getMessage());
        return ResponseEntity
                .status(e.getErrorCode().getStatus().value())
                .body(new ErrorResponseDTO(e.getErrorCode()));
    }

    /**
     * Redis
     * RedisConnectionFailureException(연결 장애)과 RedisSystemException(스크립트 실행 등 미분류 오류)은
     * 서로 형제 관계(공통 부모: DataAccessException)라 두 타입을 명시적으로 나열해서 함께 처리한다.
     */
    @ExceptionHandler({RedisConnectionFailureException.class, RedisSystemException.class})
    protected ResponseEntity<ErrorResponseDTO> handleRedisException(final DataAccessException e) {
        log.error("handleRedisException : {}", e.getMessage(), e);
        return ResponseEntity
                .status(ErrorCode.REDIS_UNAVAILABLE.getStatus().value())
                .body(new ErrorResponseDTO(ErrorCode.REDIS_UNAVAILABLE));
    }
}
