package com.cojac.storyteller.unit.common;

import com.cojac.storyteller.common.redis.RedisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 단위 테스트 클래스
 *
 * 이 클래스는 개별 단위(주로 서비스 또는 비즈니스 로직) 기능을 검증하기 위한 단위 테스트를 포함합니다.
 *
 * 주요 특징:
 * - 모의 객체(mock objects)를 사용하여 외부 의존성을 제거하고,테스트 대상 객체의 로직에만 집중합니다.
 * - 테스트의 독립성을 보장하여, 각 테스트가 서로에게 영향을 미치지 않도록 합니다.
 *
 * 테스트 전략:
 * - 간단한 기능이나 로직에 대한 테스트는 단위 테스트를 사용하십시오.
 * - 시스템의 전체적인 동작 및 상호작용을 검증하기 위해 통합 테스트를 활용하십시오.
 *
 * 참고: 단위 테스트는 실행 속도가 빠르며,
 *       전체 시스템의 동작보다는 개별 단위의 동작을 검증하는 데 중점을 둡니다.
 */
@ExtendWith(MockitoExtension.class)
class RedisServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @InjectMocks
    private RedisService redisService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);

        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
    }

    @Test
    @DisplayName("키-값 쌍을 Redis에 저장")
    void testSetValues() {
        // when
        redisService.setValues("testKey", "testValue");

        // then
        verify(valueOperations).set("testKey", "testValue");
    }

    @Test
    @DisplayName("지정된 기간 동안 키-값 쌍을 Redis에 저장")
    void testSetValuesWithDuration() {
        // when
        redisService.setValues("testKey", "testValue", Duration.ofMinutes(5));

        // then
        verify(valueOperations).set("testKey", "testValue", Duration.ofMinutes(5));
    }

    @Test
    @DisplayName("Redis에서 키에 해당하는 값을 가져옴")
    void testGetValues() {
        // given
        when(valueOperations.get("testKey")).thenReturn("testValue");

        // when
        String result = redisService.getValues("testKey");

        // then
        assertEquals("testValue", result);
    }

    @Test
    @DisplayName("Redis에서 키가 없을 때 false 반환")
    void testGetValuesNotExists() {
        // given
        when(valueOperations.get("nonExistingKey")).thenReturn(null);

        // when
        String result = redisService.getValues("nonExistingKey");

        // then
        assertEquals("false", result);
    }

    @Test
    @DisplayName("Redis에서 키-값 쌍을 삭제")
    void testDeleteValues() {
        // when
        redisService.deleteValues("testKey");

        // then
        verify(redisTemplate).delete("testKey");
    }

    @Test
    @DisplayName("키의 유효 기간을 설정")
    void testExpireValues() {
        // when
        redisService.expireValues("testKey", 1000);

        // then
        verify(redisTemplate).expire("testKey", 1000, TimeUnit.MILLISECONDS);
    }

    @Test
    @DisplayName("해시 데이터를 Redis에 저장")
    void testSetHashOps() {
        // given
        Map<String, String> hashData = Collections.singletonMap("field", "value");

        // when
        redisService.setHashOps("hashKey", hashData);

        // then
        verify(hashOperations).putAll("hashKey", hashData);
    }

    @Test
    @DisplayName("Redis에서 해시 값을 가져옴")
    void testGetHashOps() {
        // given
        when(hashOperations.hasKey("hashKey", "field")).thenReturn(true);
        when(hashOperations.get("hashKey", "field")).thenReturn("value");

        // when
        String result = redisService.getHashOps("hashKey", "field");

        // then
        assertEquals("value", result);
    }

    @Test
    @DisplayName("해시 필드 키 삭제")
    void testDeleteHashOps() {
        // when
        redisService.deleteHashOps("hashKey", "field");

        // then
        verify(hashOperations).delete("hashKey", "field");
    }

    @Test
    @DisplayName("값이 존재하는지 확인")
    void testCheckExistsValue() {
        // when
        boolean result = redisService.checkExistsValue("someValue");

        // then
        assertTrue(result);
    }

    @Test
    @DisplayName("값이 존재하지 않을 때 확인")
    void testCheckExistsValue_NotExists() {
        // when
        boolean result = redisService.checkExistsValue("false");

        // then
        assertFalse(result);
    }

    @Test
    @DisplayName("Lua 스크립트로 키 값을 조회하고 즉시 삭제")
    void getAndDelete_ShouldReturnValueAndDelete_WhenKeyExists() {
        // given
        when(redisTemplate.execute(any(), eq(List.of("testKey")))).thenReturn("storedToken");

        // when
        String result = redisService.getAndDelete("testKey");

        // then
        assertEquals("storedToken", result);
        verify(redisTemplate).execute(any(), eq(List.of("testKey")));
    }

    @Test
    @DisplayName("키가 없을 때 getAndDelete는 \"false\" 반환 (checkExistsValue NPE 방지)")
    void getAndDelete_ShouldReturnFalse_WhenKeyNotExists() {
        // given
        when(redisTemplate.execute(any(), eq(List.of("nonExistingKey")))).thenReturn(null);

        // when
        String result = redisService.getAndDelete("nonExistingKey");

        // then
        assertEquals("false", result);
    }
}
