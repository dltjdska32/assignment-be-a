package com.assginment.be_a.infra.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributeLock {
    String key();
    TimeUnit timeUnit() default TimeUnit.SECONDS;
    long waitTime() default 0L; // 0초 - 다른 서버가 락 잡고 있으면 즉시 포기 -> 스레드가 sleep 등을 하지않고 바로 풀에 반납.
    long leaseTime() default 10L; // 타임아웃 10초 - 로직 길어져도 10초 뒤면 데드락 방지용 강제 해제
}
