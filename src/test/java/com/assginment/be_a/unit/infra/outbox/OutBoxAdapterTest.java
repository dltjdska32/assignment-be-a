package com.assginment.be_a.unit.infra.outbox;

import com.assginment.be_a.application.event.CreateProductEvent;
import com.assginment.be_a.infra.outbox.OutBoxAdapter;
import com.assginment.be_a.infra.outbox.OutboxStatus;
import com.assginment.be_a.infra.outbox.ProductOutbox;
import com.assginment.be_a.infra.outbox.ProductOutboxRepo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutBoxAdapterTest {

    @Mock
    private ProductOutboxRepo outboxRepo;

    private OutBoxAdapter adapter;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        adapter = new OutBoxAdapter(outboxRepo, objectMapper);
    }

    @Test
    @DisplayName("saveEvent: 직렬화 후 ProductOutbox를 저장한다")
    void saveEvent_persistsOutbox() {
        CreateProductEvent event = new CreateProductEvent(1L, 10);

        adapter.saveEvent(event);

        ArgumentCaptor<ProductOutbox> cap = ArgumentCaptor.forClass(ProductOutbox.class);
        verify(outboxRepo).save(cap.capture());
        ProductOutbox saved = cap.getValue();
        assertThat(saved.getEventType().name()).isEqualTo("CREATE_PRODUCT");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getEventValue()).contains("\"productId\":1");
        assertThat(saved.getEventValue()).contains("\"capacity\":10");
    }

    @Test
    @DisplayName("saveEvent: 직렬화 실패 시 IllegalStateException으로 래핑")
    void saveEvent_wrapsSerializationFailure() throws JsonProcessingException {
        ObjectMapper brokenMapper = org.mockito.Mockito.mock(ObjectMapper.class);
        doAnswer(invocation -> {
            throw new RuntimeException("serialization boom");
        }).when(brokenMapper).writeValueAsString(any());
        OutBoxAdapter brokenAdapter = new OutBoxAdapter(outboxRepo, brokenMapper);
        CreateProductEvent event = new CreateProductEvent(1L, 10);

        assertThatThrownBy(() -> brokenAdapter.saveEvent(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("아웃박스 저장에 실패");
    }
}

