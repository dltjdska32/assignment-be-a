package com.assginment.be_a.infra.redis;

public abstract class RedisConst {

    static final String PRODUCT_CAPACITY_PREFIX = "product:capacity:";

    static final String PRODUCT_HOLD_ZSET_PREFIX = "product:hold:";

    /**
     * hold 만료 score(ms)보다 항상 큰 값. 결제 확정 시 멤버 score로 쓰며 ZREMRANGEBYSCORE 청소 대상이 아님.
     * (IEEE754 double 정수 정밀도 범위 내)
     */
    public static final long HOLD_SCORE_PERMANENT_MS = 9_007_199_254_740_991L;

    /**
     * ZSET 기반 임시 hold(유저별 만료).
     *
     * KEYS[1] = capacityKey (String)
     * KEYS[2] = holdZSetKey (ZSET, member=userId, score=expireAtMs)
     *
     * ARGV[1] = nowMs
     * ARGV[2] = expireAtMs (nowMs + TTL)
     * ARGV[3] = userId
     *
     * return  1 : hold 성공(이미 hold 중이면 연장 포함)
     * return  0 : 정원 초과
     * return -2 : capacityKey 없음
     */
    static final String HOLD_SEAT_SCRIPT =
            """
            -- Redis에 상품 정원이 없으면 -2 반환
            local cap = tonumber(redis.call('GET', KEYS[1]))
            if not cap then
              return -2
            end
            
            
            --permanent는 결제 확정 후 쓰는 “영구 score”와 같은 값
            local now = tonumber(ARGV[1])
            local expireAt = tonumber(ARGV[2])
            local userId = ARGV[3]
            local permanent = 9007199254740991

            
            redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', now)

            -- 유저가 ZSET에 있으면 score 문자열, 없으면 nil
            local existing = redis.call('ZSCORE', KEYS[2], userId)

            ---이미 있으면: 확정자 vs 임시 hold
            if existing then
              local ex = tonumber(existing)
              if ex >= permanent then
                return 1
              end
              redis.call('ZADD', KEYS[2], expireAt, userId)
              return 1
            end
            
            -- 없으면: 신규 hold — 정원 검사 후 추가
            local cur = tonumber(redis.call('ZCARD', KEYS[2]) or "0")
            if cur + 1 > cap then
              return 0
            end

            redis.call('ZADD', KEYS[2], expireAt, userId)
            return 1
            """;

    /**
     * 결제 확정: hold 멤버 score를 영구값으로 승격 (키 TTL 없음 가정).
     *
     * KEYS[1] = holdZSetKey
     * ARGV[1] = userId
     * ARGV[2] = permanentScore (HOLD_SCORE_PERMANENT_MS 와 동일)
     * ARGV[3] = nowMs
     *
     * return  1 : 성공(이미 확정 score면 멱등)
     * return -1 : 멤버 없음(선점 만료 등)
     */
    static final String CONFIRM_SEAT_SCRIPT =
            """
            local holdKey = KEYS[1]
            local userId = ARGV[1]
            local permanent = tonumber(ARGV[2])
            local now = tonumber(ARGV[3])

            redis.call('ZREMRANGEBYSCORE', holdKey, '-inf', now)

            local existing = redis.call('ZSCORE', holdKey, userId)
            if not existing then
              return -1
            end

            local ex = tonumber(existing)
            if ex >= permanent then
              return 1
            end

            redis.call('ZADD', holdKey, permanent, userId)
            return 1
            """;


}
