package com.assginment.be_a.application.event;

public record CreateProductEvent (
        Long productId,
        Integer capacity
) implements DomainEvent{


    @Override
    public String getEventTypeName() {
        return EventType.CREATE_PRODUCT.name();
    }

    @Override
    public EventType getEventType() {
        return EventType.CREATE_PRODUCT;
    }

    @Override
    public Long productId() {
        return this.productId;
    }
}
