package com.assginment.be_a.domain.repo;


import com.assginment.be_a.application.dto.FindProductReqDto;
import com.assginment.be_a.application.dto.FindProductRespDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ProductCustomRepo {

    Slice<FindProductRespDto> findProducts(Pageable p, FindProductReqDto dto);
}
