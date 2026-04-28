package com.assginment.be_a.infra.jwt;

import com.assginment.be_a.application.port.JwtPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class JwtAdapter implements JwtPort {

    private final JwtRepo jwtRepo;

    @Override
    public void saveRefreshToken(Long userId, String refreshToken) {
        jwtRepo.saveRefreshToken(userId, refreshToken);
    }


    @Override
    public Optional<Long> findUserId(String refreshToken) {
        return jwtRepo.findUserId(refreshToken);
    }

    @Override
    public Optional<Long> findAndDeleteRefreshToken(String refreshToken) {
        return jwtRepo.findUserIdAndDelete(refreshToken);
    }
}
