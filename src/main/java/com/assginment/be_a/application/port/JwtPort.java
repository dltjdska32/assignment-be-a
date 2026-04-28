package com.assginment.be_a.application.port;

import java.util.Optional;

public interface JwtPort {

    ///  리프레시토큰 저장.
    void saveRefreshToken(Long userId, String refreshToken);



    Optional<Long> findUserId(String refreshToken);

    Optional<Long> findAndDeleteRefreshToken(String refreshToken);

}
