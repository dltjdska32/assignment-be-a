package com.assginment.be_a.unit.infra.redis;

import com.assginment.be_a.infra.redis.ProductCacheAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCacheAdapterTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private ProductCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ProductCacheAdapter(stringRedisTemplate);
    }

    @Test
    @DisplayName("holdSeat: Lua 결과 1이면 예외 없음")
    void holdSeat_okWhenScriptReturnsOne() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        adapter.holdSeat(42L, 7L);

        verify(stringRedisTemplate).execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("holdSeat: Lua 결과 null이면 IllegalStateException")
    void holdSeat_throwsWhenNullResponse() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(null);

        assertThatThrownBy(() -> adapter.holdSeat(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis 응답이 없습니다");
    }

    @Test
    @DisplayName("holdSeat: Lua 결과 -2(capacity 없음)이면 IllegalStateException")
    void holdSeat_throwsWhenCapacityMissing() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(-2L);

        assertThatThrownBy(() -> adapter.holdSeat(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("서버내 데이터가 생성되지 않았습니다");
    }

    @Test
    @DisplayName("holdSeat: Lua 결과 0(정원 초과)이면 IllegalArgumentException")
    void holdSeat_throwsWhenFull() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(0L);

        assertThatThrownBy(() -> adapter.holdSeat(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("정원이 초과되었습니다");
    }

    @Test
    @DisplayName("holdSeat: capacity/holdZset 키에 productId가 포함된다")
    void holdSeat_passesCorrectKeysToRedis() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<String> keys = (List<String>) invocation.getArgument(1);
                    assertThat(keys).containsExactly("product:capacity:99", "product:hold:99");
                    return 1L;
                });

        adapter.holdSeat(99L, 1L);
    }

    @Test
    @DisplayName("confirmSeat: Lua 결과 1이면 예외 없음")
    void confirmSeat_okWhenScriptReturnsOne() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(1L);

        adapter.confirmSeat(42L, 7L);

        verify(stringRedisTemplate).execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("confirmSeat: Lua 결과 null이면 IllegalStateException")
    void confirmSeat_throwsWhenNullResponse() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(null);

        assertThatThrownBy(() -> adapter.confirmSeat(1L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Redis 응답이 없습니다");
    }

    @Test
    @DisplayName("confirmSeat: Lua 결과 -1이면 IllegalArgumentException")
    void confirmSeat_throwsWhenHoldMissing() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(-1L);

        assertThatThrownBy(() -> adapter.confirmSeat(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("결제 선점이 만료되었거나 없습니다");
    }

    @Test
    @DisplayName("confirmSeat: hold 키에 productId가 포함된다")
    void confirmSeat_passesHoldKeyToRedis() {
        when(stringRedisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    List<String> keys = (List<String>) invocation.getArgument(1);
                    assertThat(keys).containsExactly("product:hold:88");
                    return 1L;
                });

        adapter.confirmSeat(88L, 2L);
    }

    @Test
    @DisplayName("releaseSeat: hold ZSET에서 userId 멤버 제거")
    void releaseSeat_removesMemberFromZset() {
        @SuppressWarnings("unchecked")
        ZSetOperations<String, String> zops = mock(ZSetOperations.class);
        when(stringRedisTemplate.opsForZSet()).thenReturn(zops);

        adapter.releaseSeat(12L, 34L);

        verify(zops).remove("product:hold:12", "34");
    }
}

