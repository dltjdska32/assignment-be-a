package com.assginment.be_a.infra.redis;

import com.assginment.be_a.application.port.ProductCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

import static com.assginment.be_a.infra.redis.RedisConst.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductCacheAdapter implements ProductCachePort {
    private final StringRedisTemplate stringRedisTemplate;
    private static final DefaultRedisScript<Long> HOLD_SCRIPT =
            new DefaultRedisScript<>(HOLD_SEAT_SCRIPT, Long.class);
    private static final DefaultRedisScript<Long> CONFIRM_SCRIPT =
            new DefaultRedisScript<>(CONFIRM_SEAT_SCRIPT, Long.class);

    private static final Duration HOLD_TTL = Duration.ofMinutes(15);

    /**
     * 반환값 1 = 성공
     * 0 = 정원 초과
     * -2 = capacity 키 없음(아웃박스/캐시 미세팅)
     */
    @Override
    public void holdSeat(Long productId, Long userId) {
        String capacityKey = PRODUCT_CAPACITY_PREFIX + productId;
        String holdZsetKey = PRODUCT_HOLD_ZSET_PREFIX + productId;

        long nowMs = System.currentTimeMillis();
        long expireAtMs = nowMs + HOLD_TTL.toMillis();

        Long result = stringRedisTemplate.execute(
                HOLD_SCRIPT,
                List.of(capacityKey, holdZsetKey),
                String.valueOf(nowMs),
                String.valueOf(expireAtMs),
                String.valueOf(userId)
        );
        if (result == null) {
            throw new IllegalStateException("Redis 응답이 없습니다.");
        }
        if (result == -2L) {

            log.error("registerCourse 캐시 확인 불가 ProductId = {}", productId);
            throw new IllegalStateException("서버내 데이터가 생성되지 않았습니다. 잠시후 다시 시도해주세요. " );
        }
        if (result == 0L) {
            throw new IllegalArgumentException("정원이 초과되었습니다.");
        }
    }

    @Override
    public void confirmSeat(Long productId, Long userId) {
        String holdZsetKey = PRODUCT_HOLD_ZSET_PREFIX + productId;
        long nowMs = System.currentTimeMillis();

        Long result = stringRedisTemplate.execute(
                CONFIRM_SCRIPT,
                List.of(holdZsetKey),
                String.valueOf(userId),
                String.valueOf(HOLD_SCORE_PERMANENT_MS),
                String.valueOf(nowMs)
        );
        if (result == null) {
            throw new IllegalStateException("Redis 응답이 없습니다.");
        }
        if (result == -1L) {
            throw new IllegalArgumentException("결제 선점이 만료되었거나 없습니다.");
        }
    }

    @Override
    public void releaseSeat(Long productId, Long userId) {
        String holdZsetKey = PRODUCT_HOLD_ZSET_PREFIX + productId;
        stringRedisTemplate.opsForZSet().remove(holdZsetKey, String.valueOf(userId));
    }
}
