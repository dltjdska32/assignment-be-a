package com.assginment.be_a.unit.infra.scheduler;

import com.assginment.be_a.application.ProductService;
import com.assginment.be_a.application.event.EventType;
import com.assginment.be_a.infra.scheduler.OutBoxEventPublishScheduler;
import com.assginment.be_a.infra.outbox.OutboxStatus;
import com.assginment.be_a.infra.outbox.ProductOutbox;
import com.assginment.be_a.infra.outbox.ProductOutboxRepo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublishSchedulerTest {

    @Mock
    private ProductOutboxRepo productOutboxRepo;
    @Mock
    private ProductService productService;

    private OutBoxEventPublishScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new OutBoxEventPublishScheduler(productOutboxRepo, productService, new ObjectMapper());
    }

    private ProductOutbox pendingOutbox(long id, long productId, int capacity) {
        // 스케줄러는 CreateProductEvent(productId, capacity)로 역직렬화한다.
        // 테스트에서는 최소 JSON만 직접 생성해 역직렬화 실패를 방지한다.
        String json = "{\"productId\":" + productId + ",\"capacity\":" + capacity + "}";
        return ProductOutbox.builder()
                .id(id)
                .eventType(EventType.CREATE_PRODUCT)
                .eventValue(json)
                .status(OutboxStatus.PENDING)
                .publishedCount(0)
                .build();
    }

    @Test
    @DisplayName("publishPendingEvents: PENDING이 없으면 handleEvent 호출 없음")
    void publishPendingEvents_empty() {
        when(productOutboxRepo.findTop1000ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).thenReturn(List.of());

        scheduler.publishPendingEvents();

        verify(productService, never()).handleEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("publishPendingEvents: 성공 시 bulk PUBLISHED")
    void publishPendingEvents_successBulkPublish() {
        ProductOutbox o1 = pendingOutbox(1L, 10L, 5);
        when(productOutboxRepo.findTop1000ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).thenReturn(List.of(o1));

        scheduler.publishPendingEvents();

        verify(productService).handleEvent(org.mockito.ArgumentMatchers.any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idsCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(productOutboxRepo).bulkUpdateOutboxStatus(eq(OutboxStatus.PUBLISHED), idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(1L);
    }

    @Test
    @DisplayName("publishPendingEvents: handleEvent 실패 시 해당 outbox FAILED 단건 업데이트")
    void publishPendingEvents_marksFailedOnHandlerError() {
        ProductOutbox o1 = pendingOutbox(2L, 20L, 3);
        when(productOutboxRepo.findTop1000ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)).thenReturn(List.of(o1));
        doThrow(new RuntimeException("redis down"))
                .when(productService).handleEvent(org.mockito.ArgumentMatchers.any());

        scheduler.publishPendingEvents();

        verify(productOutboxRepo).updateOutboxStatus(OutboxStatus.FAILED, 2L);
        verify(productOutboxRepo, never()).bulkUpdateOutboxStatus(eq(OutboxStatus.PUBLISHED), eq(List.of(2L)));
    }

    @Test
    @DisplayName("publishFailedEvents: 성공 시 bulk PUBLISHED")
    void publishFailedEvents_success() {
        ProductOutbox o1 = pendingOutbox(3L, 30L, 2);
        when(productOutboxRepo.findTop1000ByStatusIsFailed()).thenReturn(List.of(o1));

        scheduler.publishFailedEvents();

        verify(productService).handleEvent(org.mockito.ArgumentMatchers.any());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idsCaptor = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        verify(productOutboxRepo).bulkUpdateOutboxStatus(eq(OutboxStatus.PUBLISHED), idsCaptor.capture());
        assertThat(idsCaptor.getValue()).containsExactly(3L);
    }

    @Test
    @DisplayName("cleanupPublishedEvents: repo delete 호출")
    void cleanupPublishedEvents_callsDelete() {
        when(productOutboxRepo.deleteOldPublishedEvents(eq(OutboxStatus.PUBLISHED), org.mockito.ArgumentMatchers.any()))
                .thenReturn(5);

        scheduler.cleanupPublishedEvents();

        verify(productOutboxRepo).deleteOldPublishedEvents(eq(OutboxStatus.PUBLISHED), org.mockito.ArgumentMatchers.any());
    }
}

