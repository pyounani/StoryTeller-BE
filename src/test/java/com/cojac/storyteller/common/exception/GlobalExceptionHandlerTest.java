package com.cojac.storyteller.common.exception;

import com.cojac.storyteller.response.code.ErrorCode;
import com.cojac.storyteller.response.dto.ErrorResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

    @Test
    @DisplayName("RedisSystemException 발생 시 503과 REDIS_UNAVAILABLE 코드로 응답")
    void handleRedisException_ShouldReturn503_ForRedisSystemException() {
        // given
        RedisSystemException e = new RedisSystemException("Redis error", new RuntimeException("cause"));

        // when
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleRedisException(e);

        // then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getStatusCode().value());
        assertEquals(ErrorCode.REDIS_UNAVAILABLE.name(), response.getBody().getCode());
    }

    @Test
    @DisplayName("RedisConnectionFailureException 발생 시에도 503과 REDIS_UNAVAILABLE 코드로 응답")
    void handleRedisException_ShouldReturn503_ForRedisConnectionFailureException() {
        // given
        RedisConnectionFailureException e = new RedisConnectionFailureException("Unable to connect to Redis");

        // when
        ResponseEntity<ErrorResponseDTO> response = globalExceptionHandler.handleRedisException(e);

        // then
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE.value(), response.getStatusCode().value());
        assertEquals(ErrorCode.REDIS_UNAVAILABLE.name(), response.getBody().getCode());
    }
}
