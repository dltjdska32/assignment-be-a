package com.assginment.be_a.domain.repo;

import com.assginment.be_a.application.dto.FindEnrollmentReqDto;
import com.assginment.be_a.application.dto.FindEnrollmentRespDto;
import com.assginment.be_a.application.dto.FindProductReqDto;
import com.assginment.be_a.application.dto.FindProductRespDto;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface EnrollmentListCustomRepo{

    Slice<FindEnrollmentRespDto> findEnrollmentInfos(Long userId, Pageable p, FindEnrollmentReqDto dto);
}
