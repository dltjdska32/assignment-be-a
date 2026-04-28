package com.assginment.be_a.domain.repo;

import com.assginment.be_a.application.dto.FindProductReqDto;
import com.assginment.be_a.application.dto.FindProductRespDto;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.assginment.be_a.domain.QProduct.product;
import static com.assginment.be_a.domain.QUser.user;

/**
 * Spring Data JPA 커스텀 구현 규칙:
 * - Repository 인터페이스명 + Impl (ex: ProductRepo -> ProductRepoImpl)
 */
@RequiredArgsConstructor
public class ProductRepoImpl implements ProductCustomRepo {

    private final JPAQueryFactory qf;

    @Override
    public Slice<FindProductRespDto> findProducts(Pageable p, FindProductReqDto dto) {
        int size = p.getPageSize();

        List<FindProductRespDto> retVal = qf
                .select(Projections.constructor(
                        FindProductRespDto.class,
                        product.id,
                        product.productName,
                        product.cost,
                        product.productState,
                        product.startDate,
                        product.endDate,
                        product.createdAt,
                        user.username
                ))
                .from(product)
                .join(product.user, user)
                .where(getConditions(dto))
                .orderBy(product.createdAt.desc(), product.id.desc())
                .limit(size + 1L)
                .fetch();

        boolean hasNext = retVal.size() > size;

        List<FindProductRespDto> vals = new ArrayList<>();
        if (hasNext) {
            vals = retVal.subList(0, size);
        } else {
            vals = retVal;
        }

        return new SliceImpl<>(vals, p, hasNext);
    }

    private BooleanExpression getConditions(FindProductReqDto dto) {
        BooleanExpression be = product.isDeleted.eq(false);

        if (dto.categoryId() != null) {
            be = be.and(product.productCategory.id.eq(dto.categoryId()));
        }

        if (dto.productState() != null) {
            be = be.and(product.productState.eq(dto.productState()));
        }

        if (dto.cursorId() != null && dto.cursorCreatedAt() != null) {
            be = be.and(getCursorCondition(dto.cursorCreatedAt(), dto.cursorId()));
        }

        return be;
    }

    private static BooleanExpression getCursorCondition(LocalDateTime cursorDate, Long cursorId) {
        return product.createdAt.lt(cursorDate)
                .or(
                        product.createdAt.eq(cursorDate)
                                .and(product.id.lt(cursorId))
                );
    }
}

