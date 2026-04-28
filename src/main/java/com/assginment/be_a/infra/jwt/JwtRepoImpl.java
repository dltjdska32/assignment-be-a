package com.assginment.be_a.infra.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

import static com.assginment.be_a.infra.config.GlobalConst.TOKEN_PREFIX;

@Repository
@RequiredArgsConstructor
public class JwtRepoImpl implements JwtRepo{

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;


    @Override
    public void saveRefreshToken(Long userId, String refreshToken) {
        String key = createKey(refreshToken);

        stringRedisTemplate.opsForValue().set(key, String.valueOf(userId), Duration.ofSeconds(refreshExpiration));
    }


    @Override
    public void deleteRefreshToken(String refreshToken) {
        String key = createKey(refreshToken);

        stringRedisTemplate.delete(key);
    }

    @Override
    public Optional<Long> findUserId(String token) {

        String key = createKey(token);
        String userId = stringRedisTemplate.opsForValue().get(key);

        return Optional.ofNullable(userId)
                .map(Long::valueOf);
    }


    @Override
    public Optional<Long> findUserIdAndDelete(String refreshToken) {

        String key = createKey(refreshToken);
        String userId = stringRedisTemplate.opsForValue().getAndDelete(key);

        return Optional.ofNullable(userId)
                .map(Long::valueOf);
    }

    private String createKey(String refreshToken) {

        return TOKEN_PREFIX + refreshToken;
    }
}
