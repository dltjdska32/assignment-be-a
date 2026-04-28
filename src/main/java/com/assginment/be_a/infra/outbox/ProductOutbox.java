package com.assginment.be_a.infra.outbox;

import com.assginment.be_a.application.event.EventType;
import com.assginment.be_a.infra.config.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class ProductOutbox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auth_outbox_id")
    private Long id;

    @Column(name = "event_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventType eventType;

    @Column(name = "event_value", columnDefinition = "TEXT", nullable = false)
    private String eventValue;  //  JSON 문자열로 저장

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxStatus status;

    private int publishedCount;

    public static ProductOutbox from(EventType eventType, String jsonEventValue) {
        return ProductOutbox.builder()
                .eventType(eventType)
                .eventValue(jsonEventValue)
                .status(OutboxStatus.PENDING)
                .build();
    }

    public void changePublishStatus() {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedCount++;
    }

    public void changeFailedStatus() {
        this.status = OutboxStatus.FAILED;
        this.publishedCount++;
    }
}
