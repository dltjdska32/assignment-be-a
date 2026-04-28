package com.assginment.be_a.integration.boot;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("로컬 MySQL/Redis 의존: 환경별로 실패 가능")
class BeAApplicationTests {

    @Test
    void contextLoads() {
    }
}

