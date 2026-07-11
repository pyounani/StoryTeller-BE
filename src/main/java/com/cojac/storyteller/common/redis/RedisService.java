package com.cojac.storyteller.common.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 키-값 쌍을 Redis에 저장
     * @param key 저장할 키
     * @param data 저장할 값
     */
    public void setValues(String key, String data) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data);
    }

    /**
     * 지정된 기간 동안 키-값 쌍을 Redis에 저장
     * @param key 저장할 키
     * @param data 저장할 값
     * @param duration 유효 기간
     */
    public void setValues(String key, String data, Duration duration) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        values.set(key, data, duration);
    }

    /**
     * Redis에서 키에 해당하는 값을 가져옴
     * @param key 가져올 키
     * @return 키에 해당하는 값, 없다면 "false" 반환
     */
    @Transactional(readOnly = true)
    public String getValues(String key) {
        ValueOperations<String, Object> values = redisTemplate.opsForValue();
        if (values.get(key) == null) {
            return "false";
        }
        return (String) values.get(key);
    }

    /**
     * Redis에서 키-값 쌍을 삭제
     * @param key 삭제할 키
     */
    public void deleteValues(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 키에 해당하는 값을 조회하고 즉시 삭제 (Lua 스크립트로 원자적 실행)
     * GET + DEL을 단일 명령으로 묶어 동시 재발급 요청의 Race Condition 방지
     * @param key 조회 및 삭제할 키
     * @return 키에 해당하는 값, 없으면 null
     */
    public String getAndDelete(String key) {
        String luaScript =
                "local val = redis.call('GET', KEYS[1])\n" +
                "if val then redis.call('DEL', KEYS[1]) end\n" +
                "return val";
        String result = (String) redisTemplate.execute(
                new DefaultRedisScript<>(luaScript, String.class),
                List.of(key)
        );
        return result != null ? result : "false";
    }

    /**
     * 키의 유효 기간을 설정
     * @param key 유효기간을 설정할 키
     * @param timeout 유효 기간(밀리초 단위)
     */
    public void expireValues(String key, int timeout) {
        redisTemplate.expire(key, timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 해시 데이터를 Redis에 저장
     * @param key 저장할 해시 키
     * @param data 저장할 해시 데이터
     */
    public void setHashOps(String key, Map<String, String> data) {
        HashOperations<String, Object, Object> values = redisTemplate.opsForHash();
        values.putAll(key, data);
    }

    /**
     * Redis에서 해시 키에 해당하는 해시 값을 가져옵니다.
     *
     * @param key     해시 키
     * @param hashKey 해시 필드 키
     * @return 해시 필드 키에 해당하는 값, 없으면 빈 문자열 반환
     */
    @Transactional(readOnly = true)
    public String getHashOps(String key, String hashKey) {
        HashOperations<String, Object, Object> values = redisTemplate.opsForHash();
        return Boolean.TRUE.equals(values.hasKey(key, hashKey)) ? (String) redisTemplate.opsForHash().get(key, hashKey) : "";
    }

    /**
     * Redis에서 해시 키-값 쌍을 삭제합니다.
     *
     * @param key     해시 키
     * @param hashKey 삭제할 해시 필드 키
     */
    public void deleteHashOps(String key, String hashKey) {
        HashOperations<String, Object, Object> values = redisTemplate.opsForHash();
        values.delete(key, hashKey);
    }

    /**
     * 값이 존재하는지 확인합니다.
     *
     * @param value 확인할 값
     * @return 값이 존재하면 true, 그렇지 않으면 false
     */
    public boolean checkExistsValue(String value) {
        return !value.equals("false");
    }
}
