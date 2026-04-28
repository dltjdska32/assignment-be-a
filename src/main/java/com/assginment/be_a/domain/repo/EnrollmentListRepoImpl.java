package com.assginment.be_a.domain.repo;

import com.assginment.be_a.application.dto.FindEnrollmentReqDto;
import com.assginment.be_a.application.dto.FindEnrollmentRespDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;

import static com.assginment.be_a.domain.QEnrollmentList.enrollmentList;
import static com.assginment.be_a.domain.QProduct.product;

/**
 * Spring Data JPA 커스텀 구현 규칙:
 * - Repository 인터페이스명 + Impl (ex: EnrollmentListRepo -> EnrollmentListRepoImpl)
 */
@RequiredArgsConstructor
public class EnrollmentListRepoImpl implements EnrollmentListCustomRepo {

    private final JPAQueryFactory qf;

    @Override
    public Slice<FindEnrollmentRespDto> findEnrollmentInfos(Long userId, Pageable p, FindEnrollmentReqDto dto) {
        int size = p.getPageSize();

        List<FindEnrollmentRespDto> retVal = qf
                .select(Projections.constructor(
                        FindEnrollmentRespDto.class,
                        product.id,
                        enrollmentList.id,
                        product.productName,
                        product.cost,
                        product.productState,
                        enrollmentList.attendanceState,
                        product.startDate,
                        product.endDate,
                        enrollmentList.createdAt
                ))
                .from(enrollmentList)
                .join(enrollmentList.product, product)
                .where(getConditions(userId, dto))
                .orderBy(enrollmentList.createdAt.desc(), enrollmentList.id.desc())
                .limit(size + 1L)
                .fetch();

        boolean hasNext = retVal.size() > size;

        List<FindEnrollmentRespDto> vals = retVal;
        if (hasNext) {
            vals = retVal.subList(0, size);
        }

        return new SliceImpl<>(vals, p, hasNext);
    }

    private BooleanExpression getConditions(Long userId, FindEnrollmentReqDto dto) {
        BooleanExpression be = enrollmentList.isDeleted.eq(false)
                .and(enrollmentList.user.id.eq(userId));

        if (dto.cursorId() != null && dto.cursorCreatedAt() != null) {
            be = be.and(getCursorCondition(dto.cursorCreatedAt(), dto.cursorId()));
        }

        return be;
    }

    private static BooleanExpression getCursorCondition(LocalDateTime cursorDate, Long cursorId) {
        return enrollmentList.createdAt.lt(cursorDate)
                .or(
                        enrollmentList.createdAt.eq(cursorDate)
                                .and(enrollmentList.id.lt(cursorId))
                );
    }
}

