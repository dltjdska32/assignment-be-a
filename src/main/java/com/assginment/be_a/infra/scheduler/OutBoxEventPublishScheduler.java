package com.assginment.be_a.infra.scheduler;


import com.assginment.be_a.application.ProductService;
import com.assginment.be_a.application.event.CreateProductEvent;
import com.assginment.be_a.application.event.DomainEvent;
import com.assginment.be_a.application.event.EventType;
import com.assginment.be_a.infra.lock.DistributeLock;
import com.assginment.be_a.infra.outbox.OutboxStatus;
import com.assginment.be_a.infra.outbox.ProductOutbox;
import com.assginment.be_a.infra.outbox.ProductOutboxRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutBoxEventPublishScheduler {

    private final ProductOutboxRepo productOutboxRepo;
    private final ProductService productService;
    private final ObjectMapper obm;

    /// pending 상태는 1초에 1번씩 시도.
    /// 분산락을 통해 서버가 늘어나도 동시성 문제 발생하지 않도록 레디슨 분산락 사용.
    @Scheduled(fixedDelay = 1000)
    @DistributeLock(key = "outbox-pending-events")
    public void publishPendingEvents() {

        List<ProductOutbox> top1000ByStatusOrderByCreatedAtAsc = productOutboxRepo
                .findTop1000ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        List<Long> productIds = new ArrayList<>();

        for (ProductOutbox productOutbox : top1000ByStatusOrderByCreatedAtAsc) {
            try {

                DomainEvent event = convertValue(productOutbox.getEventType(), productOutbox.getEventValue());

                // 펜딩로직 처리.
                productService.handleEvent(event);

                productIds.add(productOutbox.getId());

                /// 100개씩 벌크 업데이트
                if (productIds.size() == 100) {
                    // 발송 성공
                    productOutboxRepo.bulkUpdateOutboxStatus(OutboxStatus.PUBLISHED, List.copyOf(productIds));
                    // 리스트 초기화
                    productIds.clear();
                }

            } catch (Exception e) {
                // 에러 발생 시 처리
                productOutboxRepo.updateOutboxStatus(OutboxStatus.FAILED, productOutbox.getId());
                log.error("스케줄러 릴레이 역직렬화/발송 실패. outbox Id: {}", productOutbox.getId(), e);
            }
        }

        /// 잔여값 벌크업데이트
        if (!productIds.isEmpty()) {
            productOutboxRepo.bulkUpdateOutboxStatus(OutboxStatus.PUBLISHED, List.copyOf(productIds));
            productIds.clear();
        }
    }

    /// failed 상태는 5초에 1번씩 시도.
    /// 분산락을 통해 서버가 늘어나도 동시성 문제 발생하지 않도록 레디슨 분산락 사용.
    @Scheduled(fixedDelay = 5000)
    @DistributeLock(key = "outbox-failed-events")
    public void publishFailedEvents() {

        /// 이벤트 발행은 최대 5회.
        /// 발행횟수가 5회 미만인것만 조회
        List<ProductOutbox> top1000ByStatusOrderByCreatedAtAsc = productOutboxRepo
                .findTop1000ByStatusIsFailed();

        List<Long> productIds = new ArrayList<>();

        for (ProductOutbox productOutbox : top1000ByStatusOrderByCreatedAtAsc) {
            try {

                DomainEvent event = convertValue(productOutbox.getEventType(), productOutbox.getEventValue());

                // 재발행
                productService.handleEvent(event);

                productIds.add(productOutbox.getId());

                /// 100개씩 벌크 업데이트
                if (productIds.size() == 100) {
                    // 발송 성공
                    productOutboxRepo.bulkUpdateOutboxStatus(OutboxStatus.PUBLISHED, List.copyOf(productIds));
                    // 리스트 초기화
                    productIds.clear();
                }

            } catch (Exception e) {
                // 에러 발생 시 처리
                productOutboxRepo.updateOutboxStatus(OutboxStatus.FAILED, productOutbox.getId());
                log.error("스케줄러 발송 실패. outbox Id: {}", productOutbox.getId(), e);
            }
        }

        /// 잔여값 벌크업데이트
        if (!productIds.isEmpty()) {
            productOutboxRepo.bulkUpdateOutboxStatus(OutboxStatus.PUBLISHED, List.copyOf(productIds));
            productIds.clear();
        }
    }

    // 퍼블리쉬 상태 지우기 (매일 새벽 4시에 발송 성공한지 7일 지난 쓰레기 이벤트 벌크 삭제)
    @Scheduled(cron = "0 0 4 * * ?")
    @DistributeLock(key = "outbox-cleanup-events")
    public void cleanupPublishedEvents() {
        try {
            /// 7일전 데이터
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);

            int deletedCount = productOutboxRepo.deleteOldPublishedEvents(OutboxStatus.PUBLISHED, cutoffDate);
            log.info("아웃박스 발송 완료(PUBLISHED) 데이터 제거, 삭제 건수: {}", deletedCount);

        } catch (Exception e) {
            log.error("아웃박스 발송 완료(PUBLISHED) 데이터 제거중 에러 발생 (스케줄러 에러)", e);
        }
    }

    private DomainEvent convertValue(EventType eventType, String eventValue) throws JsonProcessingException {
        return switch (eventType) {
            case CREATE_PRODUCT -> obm.readValue(eventValue, CreateProductEvent.class);
            default -> throw new IllegalArgumentException("확인할 수 없는 타입 : " + eventType);
        };
    }

}
