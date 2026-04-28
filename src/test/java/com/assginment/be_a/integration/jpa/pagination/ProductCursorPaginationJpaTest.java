package com.assginment.be_a.integration.jpa.pagination;

import com.assginment.be_a.application.dto.FindProductReqDto;
import com.assginment.be_a.application.dto.FindProductRespDto;
import com.assginment.be_a.domain.Product;
import com.assginment.be_a.domain.User;
import com.assginment.be_a.domain.enums.ProductState;
import com.assginment.be_a.domain.enums.Role;
import com.assginment.be_a.domain.repo.ProductRepo;
import com.assginment.be_a.domain.repo.UserRepo;
import com.assginment.be_a.infra.config.QueryDslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(QueryDslConfig.class)
class ProductCursorPaginationJpaTest {

    @Autowired
    private ProductRepo productRepo;

    @Autowired
    private UserRepo userRepo;

    @Test
    @DisplayName("상품 조회: createdAt DESC, id DESC 커서 기반 Slice 페이지네이션 동작")
    void findProducts_cursorSlicePagination_works() {
        User creator = userRepo.save(User.builder()
                .username("creator")
                .password("pw")
                .email("creator@example.com")
                .role(Role.ROLE_CREATOR)
                .build());

        // createdAt을 명확히 세팅해서 정렬/커서 조건을 결정적으로 검증
        LocalDateTime base = LocalDateTime.of(2026, 1, 1, 0, 0, 0);
        for (int i = 0; i < 25; i++) {
            Product p = Product.builder()
                    .productName("p-" + i)
                    .description("d")
                    .user(creator)
                    .productCategory(null)
                    .cost(1000L + i)
                    .capacity(100)
                    .reservedCnt(0)
                    .startDate(base.plusDays(1))
                    .endDate(base.plusDays(2))
                    .productState(ProductState.OPEN)
                    .build();
            // 최근일수록 큰 createdAt (i가 클수록 최신)
            ReflectionTestUtils.setField(p, "createdAt", base.plusMinutes(i));
            ReflectionTestUtils.setField(p, "updatedAt", base.plusMinutes(i));
            productRepo.save(p);
        }

        PageRequest page10 = PageRequest.of(0, 10);

        Slice<FindProductRespDto> first = productRepo.findProducts(
                page10,
                new FindProductReqDto(null, null, null, null)
        );

        assertThat(first.getContent()).hasSize(10);
        assertThat(first.hasNext()).isTrue();
        assertSortedByCreatedAtDescThenIdDesc(first.getContent());

        FindProductRespDto last1 = first.getContent().get(first.getContent().size() - 1);
        Slice<FindProductRespDto> second = productRepo.findProducts(
                page10,
                new FindProductReqDto(null, last1.productId(), last1.createdAt(), null)
        );

        assertThat(second.getContent()).hasSize(10);
        assertThat(second.hasNext()).isTrue();
        assertSortedByCreatedAtDescThenIdDesc(second.getContent());

        Set<Long> ids = new HashSet<>();
        first.getContent().forEach(it -> ids.add(it.productId()));
        second.getContent().forEach(it -> ids.add(it.productId()));
        assertThat(ids).hasSize(20); // overlap 없어야 함

        FindProductRespDto last2 = second.getContent().get(second.getContent().size() - 1);
        Slice<FindProductRespDto> third = productRepo.findProducts(
                page10,
                new FindProductReqDto(null, last2.productId(), last2.createdAt(), null)
        );

        assertThat(third.getContent()).hasSize(5);
        assertThat(third.hasNext()).isFalse();
        assertSortedByCreatedAtDescThenIdDesc(third.getContent());
    }

    private static void assertSortedByCreatedAtDescThenIdDesc(List<FindProductRespDto> rows) {
        for (int i = 0; i < rows.size() - 1; i++) {
            FindProductRespDto a = rows.get(i);
            FindProductRespDto b = rows.get(i + 1);

            int cmp = a.createdAt().compareTo(b.createdAt());
            if (cmp == 0) {
                assertThat(a.productId()).isGreaterThan(b.productId());
            } else {
                assertThat(a.createdAt()).isAfterOrEqualTo(b.createdAt());
            }
        }
    }
}

