package com.assginment.be_a.application.event;

public interface DomainEvent {
    String getEventTypeName();
    EventType getEventType();
    Long productId();
}

