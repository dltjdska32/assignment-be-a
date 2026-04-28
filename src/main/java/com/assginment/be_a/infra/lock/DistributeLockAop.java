package com.assginment.be_a.infra.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Aspect
@Component
@Order(1) /// 분산락이 다른 aop (트랜잭션)보다 우선되어야함.
@RequiredArgsConstructor
public class DistributeLockAop {

    private final RedissonClient redissonClient;

    @Around("@annotation(distributeLock)")
    public Object lock(ProceedingJoinPoint joinPoint,
                       DistributeLock distributeLock) throws Throwable {
        String lockKey = "LOCK:" + distributeLock.key();

        RLock rLock = redissonClient.getLock(lockKey);

        try{
            boolean isLocked = rLock.tryLock(distributeLock.waitTime(),
                    distributeLock.leaseTime(),
                    distributeLock.timeUnit());

            ///  만약 다른 서버가 락을 걸고있으면 종료
            if(!isLocked) {
                log.info("락 획득 실패 다른 서버에서 실행 중 - {}", lockKey);
                return null;
            }

            log.info("락 획득 성공 - {}", lockKey);

            // 타겟 로직 실행
            return joinPoint.proceed();
        } catch (InterruptedException  e) {
            log.error("락 획득 인터럽트 발생", e);
            throw e;
        } finally {
            try {
                if (rLock != null && rLock.isLocked() && rLock.isHeldByCurrentThread()) {
                    rLock.unlock();
                    log.info("락 반납 - {}", lockKey);
                }
            } catch (IllegalMonitorStateException e) {
                log.warn("이미 해제되었거나 소유권 없음 - {}", lockKey);
            }
        }
    }
}
