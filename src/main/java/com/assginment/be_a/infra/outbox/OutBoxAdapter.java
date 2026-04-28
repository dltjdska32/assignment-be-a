package com.assginment.be_a.infra.outbox;

import com.assginment.be_a.application.event.DomainEvent;
import com.assginment.be_a.application.port.OutboxPort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutBoxAdapter implements OutboxPort {
    private final ProductOutboxRepo outboxRepo;
    private final ObjectMapper obm;

    @Override
    public void saveEvent(DomainEvent event) {
        try {
            String eventValue = obm.writeValueAsString(event);
            ProductOutbox box = ProductOutbox.from(event.getEventType(), eventValue);
            outboxRepo.save(box);
        } catch (Exception e) {
            // Outbox 저장 실패는 Product 저장과 함께 롤백되어야 이벤트 유실을 막을 수 있다.
            log.error("[saveEvent] 아웃박스 저장(직렬화/DB) 실패. productId: {}", event.productId(), e);
            throw new IllegalStateException("아웃박스 저장에 실패했습니다.", e);
        }
    }
}
