package com.assginment.be_a.application.port;

import com.assginment.be_a.application.event.CreateProductEvent;

public interface ProductEventPort {

    void saveCreateProductEvent(CreateProductEvent event);
}
