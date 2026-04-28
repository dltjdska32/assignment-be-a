package com.assginment.be_a.integration.concurrency;

import com.assginment.be_a.application.event.EventType;
import com.assginment.be_a.infra.outbox.OutboxStatus;
import com.assginment.be_a.infra.outbox.ProductOutbox;
import com.assginment.be_a.infra.outbox.ProductOutboxRepo;
import com.assginment.be_a.infra.scheduler.OutBoxEventPublishScheduler;
import com.assginment.be_a.support.ConcurrencyTestRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class OutboxSchedulerDistributedLockIT {

    @Autowired
    private OutBoxEventPublishScheduler scheduler;

    @Autowired
    private ProductOutboxRepo productOutboxRepo;

    @BeforeEach
    void clean() {
        productOutboxRepo.deleteAllInBatch();
    }

    @Test
    @DisplayName("Outbox 스케줄러 분산락: 여러 스레드 동시 호출에도 pending 처리는 1회만 수행된다(publishedCount=1)")
    void publishPendingEvents_isExecutedOnlyOnceAcrossConcurrentCalls() throws InterruptedException {
        // given: PENDING outbox 1건
        // scheduler.convertValue는 CreateProductEvent(productId, capacity) JSON을 기대
        String eventJson = "{\"productId\":123,\"capacity\":50}";
        ProductOutbox outbox = productOutboxRepo.save(ProductOutbox.builder()
                .eventType(EventType.CREATE_PRODUCT)
                .eventValue(eventJson)
                .status(OutboxStatus.PENDING)
                .publishedCount(0)
                .build());

        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        // when: "여러 서버"를 스레드로 시뮬레이션 (동시에 스케줄러 호출)
        ConcurrencyTestRunner.run(10, 30, i -> {
            try {
                scheduler.publishPendingEvents();
            } catch (Throwable t) {
                errors.add(t);
            }
        });

        // then: 락 덕분에 outbox는 1회만 PUBLISHED 처리되어야 한다
        assertThat(errors).isEmpty();

        Optional<ProductOutbox> reloaded = productOutboxRepo.findById(outbox.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(reloaded.get().getPublishedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Outbox 스케줄러 분산락: 여러 스레드 동시 호출에도 failed 재발행 처리는 1회만 수행된다(publishedCount=1)")
    void publishFailedEvents_isExecutedOnlyOnceAcrossConcurrentCalls() throws InterruptedException {
        // given: FAILED outbox 1건 (publishedCount < 5)
        String eventJson = "{\"productId\":234,\"capacity\":50}";
        ProductOutbox outbox = productOutboxRepo.save(ProductOutbox.builder()
                .eventType(EventType.CREATE_PRODUCT)
                .eventValue(eventJson)
                .status(OutboxStatus.FAILED)
                .publishedCount(0)
                .build());

        ConcurrentLinkedQueue<Throwable> errors = new ConcurrentLinkedQueue<>();

        // when
        ConcurrencyTestRunner.run(10, 30, i -> {
            try {
                scheduler.publishFailedEvents();
            } catch (Throwable t) {
                errors.add(t);
            }
        });

        // then
        assertThat(errors).isEmpty();

        Optional<ProductOutbox> reloaded = productOutboxRepo.findById(outbox.getId());
        assertThat(reloaded).isPresent();
        assertThat(reloaded.get().getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(reloaded.get().getPublishedCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Outbox 스케줄러 분산락: cleanup도 동시에 호출해도 delete 쿼리는 1회만 실행된다")
    void cleanupPublishedEvents_deleteQueryRunsOnlyOnceAcrossConcurrentCalls() throws InterruptedException {
        // given: 오래된 PUBLISHED 1건 + 최근 PUBLISHED 1건
        ProductOutbox oldPublished = ProductOutbox.builder()
                .eventType(EventType.CREATE_PRODUCT)
                .eventValue("{\"productId\":345,\"capacity\":50}")
                .status(OutboxStatus.PUBLISHED)
                .publishedCount(1)
                .build();
        ProductOutbox recentPublished = ProductOutbox.builder()
                .eventType(EventType.CREATE_PRODUCT)
                .eventValue("{\"productId\":346,\"capacity\":50}")
                .status(OutboxStatus.PUBLISHED)
                .publishedCount(1)
                .build();

        // createdAt은 BaseEntity에서 updatable=false 이므로 "save 후 수정"이 반영되지 않음 -> 저장 전에 세팅
        ReflectionTestUtils.setField(oldPublished, "createdAt", LocalDateTime.now().minusDays(8));
        ReflectionTestUtils.setField(recentPublished, "createdAt", LocalDateTime.now().minusDays(1));
        oldPublished = productOutboxRepo.save(oldPublished);
        recentPublished = productOutboxRepo.save(recentPublished);

        // when
        ConcurrencyTestRunner.run(10, 30, i -> scheduler.cleanupPublishedEvents());

        // then: 오래된 건만 삭제
        assertThat(productOutboxRepo.findById(oldPublished.getId())).isEmpty();
        assertThat(productOutboxRepo.findById(recentPublished.getId())).isPresent();
    }
}

