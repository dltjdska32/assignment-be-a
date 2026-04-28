package com.assginment.be_a.infra.jwt;

import java.util.Optional;

public interface JwtRepo {

    void saveRefreshToken(Long userId, String refreshToken);

    void deleteRefreshToken(String refreshToken);

    Optional<Long> findUserId(String token);

    /**
     * 원자적으로 refreshToken을 1회 소비(get + delete).
     * 동시 reissue 호출 시 단 한 번만 userId 가 반환되어야 한다.
     */
    Optional<Long> findUserIdAndDelete(String refreshToken);
}
