package com.assginment.be_a.application.port;

import com.assginment.be_a.application.event.DomainEvent;

public interface OutboxPort {
    void saveEvent(DomainEvent event);
}
