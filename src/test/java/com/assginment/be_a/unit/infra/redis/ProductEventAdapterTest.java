package com.assginment.be_a.unit.infra.redis;

import com.assginment.be_a.application.event.CreateProductEvent;
import com.assginment.be_a.infra.redis.ProductEventAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductEventAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private ProductEventAdapter adapter;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        adapter = new ProductEventAdapter(stringRedisTemplate);
    }

    @Test
    @DisplayName("saveCreateProductEvent: capacity 키에 정원 문자열 SET")
    void saveCreateProductEvent_setsCapacity() {
        CreateProductEvent event = new CreateProductEvent(5L, 25);

        adapter.saveCreateProductEvent(event);

        verify(valueOps).set("product:capacity:5", "25");
    }

    @Test
    @DisplayName("saveCreateProductEvent: capacity 키 포맷이 올바르다")
    void saveCreateProductEvent_keyFormat() {
        CreateProductEvent event = new CreateProductEvent(100L, 3);

        adapter.saveCreateProductEvent(event);

        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(valueOps).set(keyCap.capture(), eq("3"));
        assertThat(keyCap.getValue()).isEqualTo("product:capacity:100");
    }
}

