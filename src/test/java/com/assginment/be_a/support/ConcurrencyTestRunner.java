package com.assginment.be_a.support;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 동시성(레이스 컨디션) 재현을 위한 테스트 유틸.
 * - 모든 워커가 동시에 시작하도록 래치를 걸고
 * - 실행 중 발생한 예외를 수집해서 테스트를 실패시킴
 */
public final class ConcurrencyTestRunner {

    private ConcurrencyTestRunner() {
    }

    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }

    @FunctionalInterface
    public interface ThrowingIntConsumer {
        void accept(int index) throws Exception;
    }

    public record Result(int threads, int tasks, Duration elapsed, List<Throwable> errors) {
        public boolean ok() {
            return errors == null || errors.isEmpty();
        }
    }

    /**
     * 동일 작업을 tasks 횟수만큼, threads 쓰레드로 동시에 수행한다.
     */
    public static Result run(int threads, int tasks, ThrowingRunnable action) throws InterruptedException {
        Objects.requireNonNull(action, "action");
        return run(threads, tasks, i -> action.run());
    }

    /**
     * index가 필요한 경우(예: 서로 다른 userId로 실행) 사용.
     */
    public static Result run(int threads, int tasks, ThrowingIntConsumer action) throws InterruptedException {
        Objects.requireNonNull(action, "action");
        if (threads <= 0) throw new IllegalArgumentException("threads must be > 0");
        if (tasks <= 0) throw new IllegalArgumentException("tasks must be > 0");

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        // 고정 스레드풀에서는 tasks > threads 인 경우 "모든 task가 ready에 도달"할 수 없으므로
        // 풀을 최대한 포화시키는 수준(threads 만큼)만 준비되면 동시에 시작한다.
        CountDownLatch ready = new CountDownLatch(Math.min(tasks, threads));
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(tasks);

        Queue<Throwable> errors = new ConcurrentLinkedQueue<>();
        List<Future<?>> futures = new ArrayList<>(tasks);

        Instant begin = Instant.now();
        try {
            for (int i = 0; i < tasks; i++) {
                final int idx = i;
                futures.add(pool.submit(() -> {
                    if (ready.getCount() > 0) {
                        ready.countDown();
                    }
                    try {
                        start.await();
                        action.accept(idx);
                    } catch (Throwable t) {
                        errors.add(t);
                    } finally {
                        done.countDown();
                    }
                }));
            }

            // 통합 테스트(MySQL/Redis 등)는 환경에 따라 준비 시간이 길 수 있음
            if (!ready.await(30, TimeUnit.SECONDS)) {
                throw new AssertionError("동시성 테스트 준비(ready) 타임아웃: tasks=" + tasks);
            }
            start.countDown();

            if (!done.await(180, TimeUnit.SECONDS)) {
                throw new AssertionError("동시성 테스트 종료(done) 타임아웃: tasks=" + tasks);
            }
        } finally {
            pool.shutdownNow();
            // best-effort: 테스트 종료 시 워커 정리
            pool.awaitTermination(5, TimeUnit.SECONDS);
        }

        // Future에서 숨겨진 예외가 있는지 한 번 더 수집
        for (Future<?> f : futures) {
            try {
                f.get(1, TimeUnit.MILLISECONDS);
            } catch (Throwable t) {
                errors.add(t);
            }
        }

        Duration elapsed = Duration.between(begin, Instant.now());
        return new Result(threads, tasks, elapsed, List.copyOf(errors));
    }

    /**
     * 에러가 하나라도 있으면 AssertionError로 실패시킨다.
     */
    public static void assertRunOk(int threads, int tasks, ThrowingRunnable action) throws InterruptedException {
        Result result = run(threads, tasks, action);
        if (!result.ok()) {
            AssertionError ae = new AssertionError(
                    "동시성 실행 중 예외 발생: errors=" + result.errors().size()
                            + ", threads=" + threads
                            + ", tasks=" + tasks
                            + ", elapsed=" + result.elapsed());
            for (Throwable t : result.errors()) {
                ae.addSuppressed(t);
            }
            throw ae;
        }
    }
}
