package com.assginment.be_a.infra.redis;

import com.assginment.be_a.application.event.CreateProductEvent;
import com.assginment.be_a.application.port.ProductEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import static com.assginment.be_a.infra.redis.RedisConst.PRODUCT_CAPACITY_PREFIX;

@Component
@RequiredArgsConstructor
public class ProductEventAdapter implements ProductEventPort {

    private final StringRedisTemplate stringRedisTemplate;



    /// 성품 정원은 레디스를 통해 관리.
    @Override
    public void saveCreateProductEvent(CreateProductEvent event) {

        /// 상품의 정원
        String capacityKey = PRODUCT_CAPACITY_PREFIX + event.productId();

        // 정원설정.
        stringRedisTemplate.opsForValue().set(capacityKey, String.valueOf(event.capacity()));
    }
}
