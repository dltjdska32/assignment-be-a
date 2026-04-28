package com.assginment.be_a.infra.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED;
}
